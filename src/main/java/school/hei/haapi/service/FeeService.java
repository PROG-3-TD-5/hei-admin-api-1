package school.hei.haapi.service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import javax.transaction.Transactional;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import school.hei.haapi.endpoint.event.EventProducer;
import school.hei.haapi.endpoint.event.model.TypedLateFeeVerified;
import school.hei.haapi.endpoint.event.model.gen.LateFeeVerified;
import school.hei.haapi.model.BoundedPageSize;
import school.hei.haapi.model.DelayPenalty;
import school.hei.haapi.model.Fee;
import school.hei.haapi.model.PageFromOne;
import school.hei.haapi.model.validator.FeeValidator;
import school.hei.haapi.repository.DelayPenaltyRepository;
import school.hei.haapi.repository.FeeRepository;

import static org.springframework.data.domain.Sort.Direction.DESC;
import static school.hei.haapi.endpoint.rest.model.Fee.StatusEnum.LATE;
import static school.hei.haapi.endpoint.rest.model.Fee.StatusEnum.PAID;

@Service
@AllArgsConstructor
@Slf4j
public class FeeService {

    private static final school.hei.haapi.endpoint.rest.model.Fee.StatusEnum DEFAULT_STATUS = LATE;
    private final FeeRepository feeRepository;
    private final FeeValidator feeValidator;
    private final DelayPenaltyService delayPenaltyService;
    private final EventProducer eventProducer;
    private final DelayPenaltyRepository penaltyRepository;

    public static double calculateCompoundInterest(int initialAmount, int interestRate, long durationInYears) {
        double interestRateInDecimal = interestRate / 100.0;
        return initialAmount * Math.pow(1 + interestRateInDecimal, durationInYears);
    }

    public Fee getById(String id) {
        return updateFeeStatus(feeRepository.getById(id));
    }

    public Fee getByStudentIdAndFeeId(String studentId, String feeId) {
        return updateFeeStatus(feeRepository.getByStudentIdAndId(studentId, feeId));
    }

    @Transactional
    public List<Fee> saveAll(List<Fee> fees) {
        feeValidator.accept(fees);
        return feeRepository.saveAll(fees);
    }

    public List<Fee> getFees(
            PageFromOne page, BoundedPageSize pageSize,
            school.hei.haapi.endpoint.rest.model.Fee.StatusEnum status) {
        Pageable pageable =
                PageRequest.of(page.getValue() - 1, pageSize.getValue(), Sort.by(DESC, "dueDatetime"));
        if (status != null) {
            return feeRepository.getFeesByStatus(status, pageable);
        }
        return feeRepository.getFeesByStatus(DEFAULT_STATUS, pageable);
    }

    public List<Fee> getFeesByStudentId(
            String studentId, PageFromOne page, BoundedPageSize pageSize,
            school.hei.haapi.endpoint.rest.model.Fee.StatusEnum status, Instant instantUpdateValue) {
        DelayPenalty delayPenalty = delayPenaltyService.getActualDelayPenalty();
        Pageable pageable = PageRequest.of(
                page.getValue() - 1,
                pageSize.getValue(),
                Sort.by(DESC, "dueDatetime"));
        if (status != null) {
            List<Fee> fees = feeRepository.getFeesByStudentIdAndStatus(studentId, status, pageable);
            applyLateFees(fees,delayPenalty, instantUpdateValue, studentId);
            return feeRepository.getFeesByStudentIdAndStatus(studentId, status, pageable);
        }
        List<Fee> fees = feeRepository.getByStudentId(studentId, pageable);
        applyLateFees(fees, delayPenalty, instantUpdateValue, studentId);
        return feeRepository.getByStudentId(studentId, pageable);
    }
    public void applyLateFees(List<Fee> fees, DelayPenalty delayPenalty, Instant instantUpdateValue, String student_id) {
        int interestRate = delayPenalty.getInterestPercent();
        DelayPenalty delayPenalty1 = penaltyRepository.findByStudentId(student_id);
        int graceDelayInDays;
        if(delayPenalty1.getStudentId() != null){
            graceDelayInDays = delayPenalty1.getGraceDelay();
        }else {graceDelayInDays = delayPenalty.getGraceDelay();}

        int delayApplicabilityPeriodInDays = delayPenalty.getApplicabilityDelayAfterGrace();
        for (Fee fee : fees) {
            Instant dueDateTime = fee.getDueDatetime();
            Instant dayToApplyDelayPenalty = dueDateTime.plus(Duration.ofDays(graceDelayInDays));
            if (fee.getStatus() == LATE && instantUpdateValue.isAfter(dayToApplyDelayPenalty) && (fee.getUpdatedAt().isBefore(dayToApplyDelayPenalty) || fee.getUpdatedAt().isAfter(dayToApplyDelayPenalty.plus(Duration.ofDays(delayApplicabilityPeriodInDays))))) {
                System.out.println("Here");
                long daysLate = ChronoUnit.DAYS.between(dueDateTime, instantUpdateValue);
                long numberOfDaysToApplyPenalty = Math.min(daysLate - graceDelayInDays, delayApplicabilityPeriodInDays);
                System.out.println("number of days: " + numberOfDaysToApplyPenalty);
                if (numberOfDaysToApplyPenalty > 0) {
                    double lateFeeAmount = calculateCompoundInterest(fee.getRemainingAmount(), interestRate, numberOfDaysToApplyPenalty);
                    fee.setTotalAmount((int) lateFeeAmount);
                    fee.setRemainingAmount((int) lateFeeAmount);
                    fee.setUpdatedAt(instantUpdateValue);
                }
            } else if (fee.getStatus() == LATE && instantUpdateValue.isAfter(dayToApplyDelayPenalty) && fee.getUpdatedAt().isAfter(dayToApplyDelayPenalty) && fee.getUpdatedAt().isBefore(dayToApplyDelayPenalty.plus(Duration.ofDays(delayApplicabilityPeriodInDays)))) {
                long daysLate = ChronoUnit.DAYS.between(dueDateTime, instantUpdateValue);
                long daysBetweenUpdateAndDaytoApplyDelayPenalty = ChronoUnit.DAYS.between(dayToApplyDelayPenalty, fee.getUpdatedAt());
                long numberOfDaysToApplyPenalty = Math.min((daysLate - graceDelayInDays) - daysBetweenUpdateAndDaytoApplyDelayPenalty, delayApplicabilityPeriodInDays - daysBetweenUpdateAndDaytoApplyDelayPenalty);
                if (numberOfDaysToApplyPenalty > 0) {
                    double lateFeeAmount = calculateCompoundInterest(fee.getRemainingAmount(), interestRate, numberOfDaysToApplyPenalty);
                    fee.setTotalAmount((int) lateFeeAmount);
                    fee.setRemainingAmount((int) lateFeeAmount);
                    fee.setUpdatedAt(instantUpdateValue);
                }
            }
        }
        feeRepository.saveAll(fees);
    }

    private Fee updateFeeStatus(Fee initialFee) {
        if (initialFee.getRemainingAmount() == 0) {
            initialFee.setStatus(PAID);
        } else if (Instant.now().isAfter(initialFee.getDueDatetime())) {
            initialFee.setStatus(LATE);
        }
        return initialFee;
    }

    @Scheduled(cron = "0 0 * * * *")
    public void updateFeesStatusToLate() {
        List<Fee> unpaidFees = feeRepository.getUnpaidFees();
        unpaidFees.forEach(fee -> {
            updateFeeStatus(fee);
            log.info("Fee with id." + fee.getId() + " is going to be updated from UNPAID to LATE");
        });
        feeRepository.saveAll(unpaidFees);
    }

    private TypedLateFeeVerified toTypedEvent(Fee fee) {
        return new TypedLateFeeVerified(
                LateFeeVerified.builder()
                        .type(fee.getType())
                        .student(fee.getStudent())
                        .comment(fee.getComment())
                        .remainingAmount(fee.getRemainingAmount())
                        .dueDatetime(fee.getDueDatetime())
                        .build()
        );
    }

    /*
     * An email will be sent to user with late fees
     * every morning at 8am (UTC+3)
     * */
    @Scheduled(cron = "0 0 8 * * *")
    public void sendLateFeesEmail() {
        List<Fee> lateFees = feeRepository.getFeesByStatus(LATE);
        lateFees.forEach(
                fee -> {
                    eventProducer.accept(List.of(toTypedEvent(fee)));
                    log.info("Late Fee with id." + fee.getId() + " is sent to Queue");
                }
        );
    }

}