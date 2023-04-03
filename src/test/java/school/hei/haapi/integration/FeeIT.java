package school.hei.haapi.integration;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;
import school.hei.haapi.SentryConf;
import school.hei.haapi.endpoint.rest.api.PayingApi;
import school.hei.haapi.endpoint.rest.client.ApiClient;
import school.hei.haapi.endpoint.rest.client.ApiException;
import school.hei.haapi.endpoint.rest.model.*;
import school.hei.haapi.endpoint.rest.security.cognito.CognitoComponent;
import school.hei.haapi.integration.conf.AbstractContextInitializer;
import school.hei.haapi.integration.conf.TestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static school.hei.haapi.integration.conf.TestUtils.*;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = FeeIT.ContextInitializer.class)
@AutoConfigureMockMvc
class FeeIT {
  @MockBean
  private SentryConf sentryConf;
  @MockBean
  private CognitoComponent cognitoComponentMock;

  private static ApiClient anApiClient(String token) {
    return TestUtils.anApiClient(token, FeeIT.ContextInitializer.SERVER_PORT);
  }

  static school.hei.haapi.model.User user(){
    school.hei.haapi.model.User user = new school.hei.haapi.model.User();
    user.setId("studentTest");
    user.setFirstName("Test");
    user.setLastName("Test");
    user.setEmail("test@gmail.com");
    user.setRef("STDTest");
    user.setStatus(school.hei.haapi.model.User.Status.valueOf("ENABLED"));
    user.setSex(school.hei.haapi.model.User.Sex.M);
    user.setBirthDate(LocalDate.parse("2000-01-01"));
    user.setPhone("0322411123");
    user.setAddress("Adr 1");
    user.setRole(school.hei.haapi.model.User.Role.STUDENT);
    user.setGraceDelay(15);
    return user;
  }
  static Fee fee1() {
    Fee fee = new Fee();
    fee.setId(FEE1_ID);
    fee.setStudentId(STUDENT1_ID);
    fee.setStatus(Fee.StatusEnum.PAID);
    fee.setType(Fee.TypeEnum.TUITION);
    fee.setTotalAmount(5000);
    fee.setRemainingAmount(0);
    fee.updatedAt(Instant.parse("2023-02-08T08:30:24.00Z"));
    fee.setComment("Comment");
    fee.creationDatetime(Instant.parse("2021-11-08T08:25:24.00Z"));
    fee.setDueDatetime(Instant.parse("2021-12-08T08:25:24.00Z"));
    return fee;
  }

  static Fee fee2() {
    Fee fee = new Fee();
    fee.setId(FEE2_ID);
    fee.setStudentId(STUDENT1_ID);
    fee.setStatus(Fee.StatusEnum.PAID);
    fee.setType(Fee.TypeEnum.HARDWARE);
    fee.setTotalAmount(5000);
    fee.setRemainingAmount(0);
    fee.setComment("Comment");
    fee.setUpdatedAt(Instant.parse("2023-02-08T08:30:24Z"));
    fee.creationDatetime(Instant.parse("2021-11-10T08:25:24.00Z"));
    fee.setDueDatetime(Instant.parse("2021-12-10T08:25:24.00Z"));
    return fee;
  }

  static Fee fee3() {
    Fee fee = new Fee();
    fee.setId(FEE3_ID);
    fee.setStudentId(STUDENT1_ID);
    fee.setStatus(Fee.StatusEnum.LATE);
    fee.setType(Fee.TypeEnum.TUITION);
    fee.setTotalAmount(6094);
    fee.setRemainingAmount(6094);
    fee.setComment("Comment");
    fee.setUpdatedAt(Instant.now());
    fee.creationDatetime(Instant.parse("2022-12-08T08:25:24.00Z"));
    fee.setDueDatetime(Instant.parse("2021-12-09T08:25:24.00Z"));
    return fee;
  }

  static Fee fee4() {
    Fee fee = new Fee();
    fee.setId(FEE4_ID);
    fee.setStudentId(STUDENT1_ID);
    fee.setStatus(Fee.StatusEnum.LATE);
    fee.setType(Fee.TypeEnum.TUITION);
    fee.setTotalAmount(5253);
    fee.setRemainingAmount(5253);
    fee.setComment("Comment");
    fee.setUpdatedAt(Instant.now());
    fee.creationDatetime(Instant.parse("2022-12-08T08:25:24.00Z"));
    fee.setDueDatetime(Instant.parse("2021-12-09T08:25:24.00Z"));
    return fee;
  }

  static CreateFee creatableFee1() {
    return new CreateFee()
        .type(CreateFee.TypeEnum.TUITION)
        .totalAmount(5000)
        .comment("Comment")
        .dueDatetime(Instant.parse("2021-12-08T08:25:24.00Z"));
  }

  @BeforeEach
  void setUp() {
    setUpCognito(cognitoComponentMock);
  }

  @Test
  void student_read_ok() throws ApiException {
    ApiClient student1Client = anApiClient(STUDENT1_TOKEN);
    PayingApi api = new PayingApi(student1Client);

    // Fee actualFee = api.getStudentFeeById(STUDENT1_ID, FEE1_ID);
    CreateDelayPenaltyChange newDelay = new CreateDelayPenaltyChange().graceDelay(5).interestPercent(3).interestTimerate(CreateDelayPenaltyChange.InterestTimerateEnum.valueOf("DAILY"))
            .applicabilityDelayAfterGrace(15);
    api.createDelayPenaltyChange(newDelay);
    List<Fee> actual = api.getStudentFees(STUDENT1_ID, 1, 5, null);

    // assertEquals(fee1(), actualFee);
    assertTrue(actual.contains(fee1()));
    assertTrue(actual.contains(fee2()));
    /*assertEquals(fee3().getTotalAmount(),actual.get(1).getTotalAmount());*/
    assertEquals(fee4().getTotalAmount(),actual.get(0).getTotalAmount());
  }

  @Test
  void student_read_with_grace_ok() throws ApiException {
    ApiClient studentTestClient = anApiClient(STUDENTTEST_TOKEN);
    PayingApi api = new PayingApi(studentTestClient);

    List<Fee> actual = api.getStudentFees(STUDENTTEST_TOKEN, 1, 5, null);

    assertEquals(fee4().getTotalAmount(),actual.get(0).getTotalAmount());
  }


  @Test
  void manager_read_ok() throws ApiException {
    ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
    PayingApi api = new PayingApi(manager1Client);

    Fee actualFee = api.getStudentFeeById(STUDENT1_ID, FEE1_ID);
    List<Fee> actualFees1 = api.getStudentFees(STUDENT1_ID, 1, 5, null);
    List<Fee> actualFees2 = api.getFees(String.valueOf(Fee.StatusEnum.PAID), 1, 10);

    assertEquals(fee1(), actualFee);
    assertEquals(fee3().getTotalAmount(),actualFees1.get(2).getTotalAmount());
    assertEquals(2, actualFees2.size());
    assertTrue(actualFees1.contains(fee1()));
    assertTrue(actualFees1.contains(fee2()));
    assertTrue(actualFees2.contains(fee1()));
    assertTrue(actualFees2.contains(fee2()));
  }

  @Test
  void student_read_ko() {
    ApiClient student1Client = anApiClient(STUDENT1_TOKEN);
    PayingApi api = new PayingApi(student1Client);

    assertThrowsApiException(
        "{\"type\":\"403 FORBIDDEN\",\"message\":\"Access is denied\"}",
        () -> api.getStudentFeeById(STUDENT2_ID, FEE2_ID));
    assertThrowsApiException(
        "{\"type\":\"403 FORBIDDEN\",\"message\":\"Access is denied\"}",
        () -> api.getStudentFees(STUDENT2_ID, null, null, null));
    assertThrowsApiException(
        "{\"type\":\"403 FORBIDDEN\",\"message\":\"Access is denied\"}",
        () -> api.getFees(null, null, null));
  }

  @Test
  void teacher_read_ko() {
    ApiClient teacher1Client = anApiClient(TEACHER1_TOKEN);
    PayingApi api = new PayingApi(teacher1Client);

    assertThrowsApiException(
        "{\"type\":\"403 FORBIDDEN\",\"message\":\"Access is denied\"}",
        () -> api.getStudentFeeById(STUDENT2_ID, FEE2_ID));
    assertThrowsApiException(
        "{\"type\":\"403 FORBIDDEN\",\"message\":\"Access is denied\"}",
        () -> api.getStudentFees(STUDENT2_ID, null, null, null));
    assertThrowsApiException(
        "{\"type\":\"403 FORBIDDEN\",\"message\":\"Access is denied\"}",
        () -> api.getFees(null, null, null));
  }

  @Test
  void manager_write_ok() throws ApiException {
    ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
    PayingApi api = new PayingApi(manager1Client);

    List<Fee> actual = api.createStudentFees(STUDENT1_ID, List.of(creatableFee1()));

    List<Fee> expected = api.getStudentFees(STUDENT1_ID, 1, 5, null);
    assertTrue(expected.containsAll(actual));
  }

  @Test
  void student_write_ko() {
    ApiClient student1Client = anApiClient(STUDENT1_TOKEN);
    PayingApi api = new PayingApi(student1Client);

    assertThrowsApiException(
        "{\"type\":\"403 FORBIDDEN\",\"message\":\"Access is denied\"}",
        () -> api.createStudentFees(STUDENT1_ID, List.of()));
  }

  @Test
  void teacher_write_ko() {
    ApiClient teacher1Client = anApiClient(TEACHER1_TOKEN);
    PayingApi api = new PayingApi(teacher1Client);

    assertThrowsApiException(
        "{\"type\":\"403 FORBIDDEN\",\"message\":\"Access is denied\"}",
        () -> api.createStudentFees(STUDENT1_ID, List.of()));
  }

  @Test
  void manager_write_with_some_bad_fields_ko() throws ApiException {
    ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
    PayingApi api = new PayingApi(manager1Client);
    CreateFee toCreate1 = creatableFee1().totalAmount(null);
    CreateFee toCreate2 = creatableFee1().totalAmount(-1);
    CreateFee toCreate3 = creatableFee1().dueDatetime(null);
    List<Fee> expected = api.getStudentFees(STUDENT1_ID, 1, 5, null);

    ApiException exception1 = assertThrows(ApiException.class,
        () -> api.createStudentFees(STUDENT1_ID, List.of(toCreate1)));
    ApiException exception2 = assertThrows(ApiException.class,
        () -> api.createStudentFees(STUDENT1_ID, List.of(toCreate2)));
    ApiException exception3 = assertThrows(ApiException.class,
        () -> api.createStudentFees(STUDENT1_ID, List.of(toCreate3)));

    List<Fee> actual = api.getStudentFees(STUDENT1_ID, 1, 5, null);
    assertEquals(expected.size(), actual.size());
    assertTrue(expected.containsAll(actual));
    String exceptionMessage1 = exception1.getMessage();
    String exceptionMessage2 = exception2.getMessage();
    String exceptionMessage3 = exception3.getMessage();
    assertTrue(exceptionMessage1.contains("Total amount is mandatory"));
    assertTrue(exceptionMessage2.contains("Total amount must be positive"));
    assertTrue(exceptionMessage3.contains("Due datetime is mandatory"));
  }

  static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
