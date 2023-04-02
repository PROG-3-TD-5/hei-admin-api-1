package school.hei.haapi.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import school.hei.haapi.model.DelayPenalty;
import school.hei.haapi.model.Fee;
import school.hei.haapi.repository.DelayPenaltyRepository;
import school.hei.haapi.repository.FeeRepository;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@AllArgsConstructor
public class DelayPenaltyService {
    private final FeeRepository feeRepository;
    private final FeeService feeService;
    private final DelayPenaltyService delayPenaltyService;
    private final DelayPenaltyRepository delayPenaltyRepository;

    public DelayPenalty getActualDelayPenalty() {
        List<DelayPenalty> penalties = delayPenaltyRepository.findAll();
        if (penalties.isEmpty()) {
            throw new NoSuchElementException("No delay penalty found");
        }
        final int FIRST_PENALTY_INDEX = 0;
        return penalties.get(FIRST_PENALTY_INDEX);
    }

    public DelayPenalty modifyActualPenalty(DelayPenalty delayPenalty) {
        if (delayPenalty == null) {
            throw new IllegalArgumentException("Cannot modify null DelayPenalty object");
        }
        DelayPenalty newDelayPenalty = delayPenaltyRepository.save(delayPenalty);
        List<Fee> fees = feeRepository.findAll();
        DelayPenalty actualDelayPenalty = delayPenaltyService.getActualDelayPenalty();
        feeService.applyLateFees(fees, actualDelayPenalty, Instant.parse("'2023-03-16T08:30:00.00Z'"));
        return newDelayPenalty;
    }
}
