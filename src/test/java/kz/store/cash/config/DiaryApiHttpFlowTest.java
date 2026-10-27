package kz.store.cash.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Optional;
import kz.store.cash.config.ApiClientsForRestConfig.UnauthorizedException;
import kz.store.cash.handler.BusinessException;
import kz.store.cash.handler.ExternalClientException;
import kz.store.cash.handler.ExternalResponseError;
import kz.store.cash.handler.ExternalServerException;
import kz.store.cash.model.diarydebt.DiaryCustomer;
import kz.store.cash.model.diarydebt.DiaryTransaction;
import kz.store.cash.model.entity.AppSetting;
import kz.store.cash.model.enums.DiaryOperationType;
import kz.store.cash.service.AppSettingService;
import kz.store.cash.service.diary.DebtDiaryExecutor;
import kz.store.cash.service.diary.DiaryDebtAuthService;
import kz.store.cash.service.diary.DiaryDebtCustomerApi;
import kz.store.cash.service.diary.DiaryTransactionApi;
import kz.store.cash.service.diary.TokenHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * Сквозная проверка клиентов Книги задолженности: настоящие RestClient'ы с боевыми обработчиками
 * статусов из {@link RestConfiguration}, настоящие executor/auth/TokenHolder, HTTP подменён.
 * Лежит в пакете config, потому что обработчики статусов package-private.
 */
class DiaryApiHttpFlowTest {

  static final String BASE = "http://diary.local";
  static final String TOKEN_URL = BASE + "/token";
  static final String CUSTOMERS = BASE + "/api/customers";
  static final String TRANSACTIONS = BASE + "/api/transactions";

  TokenHolder tokenHolder = new TokenHolder();
  MockRestServiceServer server;
  DiaryDebtCustomerApi customerApi;
  DiaryTransactionApi transactionApi;

  @BeforeEach void setUp() {
    RestClient.Builder base = RestClient.builder();
    new RestConfiguration().commonStatusHandlersCustomizer().customize(base);
    server = MockRestServiceServer.bindTo(base).build();

    ApiClientsForRestConfig config = new ApiClientsForRestConfig();
    RestClient raw = config.rawHydraClient(base);
    RestClient hydra = config.hydraClient(base, tokenHolder);

    AppSetting setting = new AppSetting();
    setting.setDebtDiaryBaseAddress(BASE);
    setting.setDebtDiaryLogin("cashier");
    setting.setDebtDiaryPassword("pwd");
    AppSettingService settings = mock(AppSettingService.class);
    when(settings.getSingleton()).thenReturn(Optional.of(setting));

    DiaryDebtAuthService auth = new DiaryDebtAuthService(raw, tokenHolder, settings);
    DebtDiaryExecutor exec = new DebtDiaryExecutor(auth, tokenHolder);
    customerApi = new DiaryDebtCustomerApi(hydra, exec, auth, settings);
    transactionApi = new DiaryTransactionApi(hydra, exec, auth, settings);
  }

  // ---------- авторизация ----------

  @Test void logsInWhenThereIsNoTokenAndSendsItAsBearer() {
    expectLogin("tok-1");
    server.expect(requestTo(startsWith(CUSTOMERS)))
        .andExpect(header("Authorization", "Bearer tok-1"))
        .andRespond(json("{\"hydra:member\":[],\"hydra:totalItems\":0}"));

    customerApi.getPage(1, null, null);

    server.verify();
    assertThat(tokenHolder.get()).isEqualTo("tok-1");
  }

  @Test void existingTokenIsReusedWithoutLogin() {
    tokenHolder.set("cached");
    server.expect(requestTo(startsWith(CUSTOMERS)))
        .andExpect(header("Authorization", "Bearer cached"))
        .andRespond(json("{\"hydra:member\":[]}"));

    customerApi.getPage(1, null, null);

    server.verify();
  }

  @Test void expiredTokenOn401TriggersReloginAndOneRetryWithNewToken() {
    tokenHolder.set("old");
    server.expect(once(), requestTo(startsWith(CUSTOMERS)))
        .andExpect(header("Authorization", "Bearer old"))
        .andRespond(withStatus(HttpStatus.UNAUTHORIZED));
    expectLogin("fresh");
    server.expect(once(), requestTo(startsWith(CUSTOMERS)))
        .andExpect(header("Authorization", "Bearer fresh"))
        .andRespond(json("{\"hydra:member\":[{\"id\":1,\"name\":\"Ivan\"}],\"hydra:totalItems\":1}"));

    var page = customerApi.getPage(1, null, null);

    server.verify();
    assertThat(page.content()).singleElement().satisfies(c -> assertThat(c.name()).isEqualTo("Ivan"));
    assertThat(tokenHolder.get()).isEqualTo("fresh");
  }

  @Test void forbiddenAndSessionExpiredStatusesAlsoTriggerRelogin() {
    for (HttpStatus status : new HttpStatus[]{HttpStatus.FORBIDDEN}) {
      server.reset();
      tokenHolder.set("old");
      server.expect(once(), requestTo(startsWith(CUSTOMERS))).andRespond(withStatus(status));
      expectLogin("fresh");
      server.expect(once(), requestTo(startsWith(CUSTOMERS))).andRespond(json("{\"hydra:member\":[]}"));

      customerApi.getPage(1, null, null);

      server.verify();
    }
  }

  @Test void secondUnauthorizedAfterReloginIsReportedNotRetriedForever() {
    tokenHolder.set("old");
    server.expect(once(), requestTo(startsWith(CUSTOMERS))).andRespond(withStatus(HttpStatus.UNAUTHORIZED));
    expectLogin("fresh");
    server.expect(once(), requestTo(startsWith(CUSTOMERS))).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

    assertThatThrownBy(() -> customerApi.getPage(1, null, null)).isInstanceOf(UnauthorizedException.class);

    server.verify();
  }

  @Test void loginResponseWithoutTokenIsRejected() {
    server.expect(requestTo(TOKEN_URL)).andRespond(json("{\"id\":1}"));

    assertThatThrownBy(() -> customerApi.getPage(1, null, null)).isInstanceOf(ExternalResponseError.class);
    assertThat(tokenHolder.isPresent()).isFalse();
  }

  @Test void wrongCredentialsOnLoginAreReportedAsClientError() {
    server.expect(requestTo(TOKEN_URL)).andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("bad credentials"));

    assertThatThrownBy(() -> customerApi.getPage(1, null, null))
        .isInstanceOf(ExternalClientException.class)
        .hasMessageContaining("bad credentials");
  }

  // ---------- ошибки ответа ----------

  @Test void clientErrorCarriesStatusUrlAndBody() {
    tokenHolder.set("t");
    server.expect(requestTo(startsWith(CUSTOMERS)))
        .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY).body("{\"detail\":\"bad page\"}"));

    assertThatThrownBy(() -> customerApi.getPage(1, null, null))
        .isInstanceOf(ExternalClientException.class)
        .hasMessageContaining("422").hasMessageContaining("/api/customers").hasMessageContaining("bad page");
  }

  @Test void notFoundIsClientErrorWithoutReloginAttempt() {
    tokenHolder.set("t");
    server.expect(once(), requestTo(startsWith(CUSTOMERS))).andRespond(withStatus(HttpStatus.NOT_FOUND));

    assertThatThrownBy(() -> customerApi.getPage(1, null, null)).isInstanceOf(ExternalClientException.class);

    server.verify();
  }

  @Test void serverErrorBecomesExternalServerExceptionAndIsNotRetried() {
    tokenHolder.set("t");
    server.expect(once(), requestTo(startsWith(CUSTOMERS)))
        .andRespond(withStatus(HttpStatus.BAD_GATEWAY).body("upstream down"));

    assertThatThrownBy(() -> customerApi.getPage(1, null, null))
        .isInstanceOf(ExternalServerException.class)
        .hasMessageContaining("502").hasMessageContaining("upstream down");

    server.verify();
    assertThat(tokenHolder.get()).isEqualTo("t");
  }

  @Test void emptyBodyIsReportedClearly() {
    tokenHolder.set("t");
    server.expect(requestTo(startsWith(CUSTOMERS))).andRespond(withSuccess());

    assertThatThrownBy(() -> customerApi.getPage(1, null, null))
        .isInstanceOf(IllegalStateException.class).hasMessageContaining("Пустой ответ");
  }

  // ---------- список клиентов: запрос и разбор страницы ----------

  @Test void buildsQueryWithSearchAndOrderAndEncodesIt() {
    tokenHolder.set("t");
    server.expect(query(q -> assertThat(q).contains("page=2").contains("search=Иван Петров")
            .contains("order[total]=desc")))
        .andRespond(json("{\"hydra:member\":[]}"));

    customerApi.getPage(2, "  Иван Петров ", "-total");

    server.verify();
  }

  @Test void orderNamesAreMappedAndUnknownFallsBackToNameAscending() {
    tokenHolder.set("t");
    String[][] cases = {{"place", "order[place]=asc"}, {"-lastTransactionAt", "order[last_transaction]=desc"},
        {"last_transaction", "order[last_transaction]=asc"}, {"whatever", "order[name]=asc"},
        {"-name", "order[name]=desc"}};
    for (String[] c : cases) {
      server.reset();
      server.expect(query(q -> assertThat(q).contains(c[1]))).andRespond(json("{\"hydra:member\":[]}"));
      customerApi.getPage(1, null, c[0]);
      server.verify();
    }
  }

  @Test void blankSearchAndOrderAreOmittedAndPageIsClampedToOne() {
    tokenHolder.set("t");
    server.expect(query(q -> assertThat(q).isEqualTo("page=1")))
        .andRespond(json("{\"hydra:member\":[]}"));

    customerApi.getPage(-5, "   ", " ");

    server.verify();
  }

  @Test void parsesHydraPagingFromViewLinks() {
    tokenHolder.set("t");
    server.expect(requestTo(startsWith(CUSTOMERS))).andRespond(json("""
        {"hydra:totalItems": 57,
         "hydra:member": [{"id":1,"name":"A","total":"100"},{"id":2,"name":"B","total":"0"}],
         "hydra:view": {"@id":"/api/customers?page=2","hydra:last":"/api/customers?page=6",
                        "hydra:next":"/api/customers?page=3","hydra:previous":"/api/customers?page=1"}}
        """));

    var page = customerApi.getPage(2, null, null);

    assertThat(page.page()).isEqualTo(2);
    assertThat(page.totalPages()).isEqualTo(6);
    assertThat(page.totalItems()).isEqualTo(57);
    assertThat(page.next()).isEqualTo("/api/customers?page=3");
    assertThat(page.previous()).isEqualTo("/api/customers?page=1");
    assertThat(page.content()).extracting(DiaryCustomer::name).containsExactly("A", "B");
  }

  @Test void missingViewAndMembersFallBackToRequestedPageAndEmptyList() {
    tokenHolder.set("t");
    server.expect(requestTo(startsWith(CUSTOMERS))).andRespond(json("{}"));

    var page = customerApi.getPage(4, null, null);

    assertThat(page.content()).isEmpty();
    assertThat(page.page()).isEqualTo(4);
    assertThat(page.totalPages()).isEqualTo(4);
    assertThat(page.totalItems()).isZero();
    assertThat(page.next()).isNull();
  }

  @Test void unknownJsonFieldsAreIgnored() {
    tokenHolder.set("t");
    server.expect(requestTo(startsWith(CUSTOMERS))).andRespond(json("""
        {"@context":"/api/contexts/Customer","hydra:member":[{"id":1,"name":"A","extra":{"x":1}}]}
        """));

    assertThat(customerApi.getPage(1, null, null).content()).hasSize(1);
  }

  // ---------- транзакции ----------

  @Test void createSalePostsDebtTransactionWithTypeCustomerAndAuditComment() {
    tokenHolder.set("t");
    server.expect(requestTo(TRANSACTIONS)).andExpect(method(HttpMethod.POST))
        .andExpect(header("Authorization", "Bearer t"))
        .andExpect(jsonPath("$.amount").value(1500))
        .andExpect(jsonPath("$.type").value("/api/types/2"))
        .andExpect(jsonPath("$.customer").value("/api/customers/7"))
        .andExpect(jsonPath("$.comment").value("Реализация(Долг) записан с кассы по логину: cashier"))
        .andRespond(withStatus(HttpStatus.CREATED));

    var response = transactionApi.createSale(new DiaryTransaction("7", 1500), DiaryOperationType.DEBT_SALE);

    server.verify();
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
  }

  @Test void createReturnUsesReturnTypeAndDoesNotDoublePrefixCustomer() {
    tokenHolder.set("t");
    server.expect(requestTo(TRANSACTIONS))
        .andExpect(jsonPath("$.type").value("/api/types/3"))
        .andExpect(jsonPath("$.customer").value("/api/customers/7"))
        .andExpect(jsonPath("$.comment").value("Возврат записан с кассы по логину: cashier"))
        .andRespond(withStatus(HttpStatus.CREATED));

    transactionApi.createSale(new DiaryTransaction("/api/customers/7", 10), DiaryOperationType.RETURN);

    server.verify();
  }

  @Test void createSaleRetriesTheSamePayloadAfterRelogin() {
    tokenHolder.set("old");
    server.expect(once(), requestTo(TRANSACTIONS)).andRespond(withStatus(HttpStatus.UNAUTHORIZED));
    expectLogin("fresh");
    server.expect(once(), requestTo(TRANSACTIONS))
        .andExpect(header("Authorization", "Bearer fresh"))
        .andExpect(jsonPath("$.amount").value(300))
        .andExpect(jsonPath("$.type").value("/api/types/2"))
        .andRespond(withStatus(HttpStatus.CREATED));

    transactionApi.createSale(new DiaryTransaction("5", 300), DiaryOperationType.DEBT_SALE);

    server.verify();
  }

  @Test void createSaleRejectedByDiaryIsClientErrorWithBody() {
    tokenHolder.set("t");
    server.expect(requestTo(TRANSACTIONS))
        .andRespond(withStatus(HttpStatus.BAD_REQUEST).body("customer not found"));

    assertThatThrownBy(() -> transactionApi.createSale(new DiaryTransaction("9", 1), DiaryOperationType.DEBT_SALE))
        .isInstanceOf(ExternalClientException.class).hasMessageContaining("customer not found");
  }

  @Test void createSaleOnDiaryOutageIsExternalServerException() {
    tokenHolder.set("t");
    server.expect(once(), requestTo(TRANSACTIONS)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

    assertThatThrownBy(() -> transactionApi.createSale(new DiaryTransaction("9", 1), DiaryOperationType.DEBT_SALE))
        .isInstanceOf(ExternalServerException.class);

    server.verify();
  }

  @Test void createSaleFailsWhenBaseAddressIsBlank() {
    AppSetting blank = new AppSetting();
    blank.setDebtDiaryBaseAddress(" ");
    blank.setDebtDiaryLogin("l"); blank.setDebtDiaryPassword("p");
    AppSettingService settings = mock(AppSettingService.class);
    when(settings.getSingleton()).thenReturn(Optional.of(blank));
    tokenHolder.set("t");
    DiaryDebtAuthService auth = mock(DiaryDebtAuthService.class);
    DiaryTransactionApi api = new DiaryTransactionApi(RestClient.create(), new DebtDiaryExecutor(auth, tokenHolder),
        auth, settings);

    assertThatThrownBy(() -> api.createSale(new DiaryTransaction("1", 1), DiaryOperationType.DEBT_SALE))
        .isInstanceOf(BusinessException.class).hasMessageContaining("base адрес");
  }

  // ---------- helpers ----------

  private void expectLogin(String token) {
    server.expect(once(), requestTo(TOKEN_URL)).andExpect(method(HttpMethod.POST))
        .andExpect(jsonPath("$.username").value("cashier"))
        .andExpect(jsonPath("$.password").value("pwd"))
        .andRespond(json("{\"token\":\"" + token + "\"}"));
  }

  private static org.springframework.test.web.client.response.DefaultResponseCreator json(String body) {
    return withSuccess(body, MediaType.APPLICATION_JSON);
  }

  private static org.hamcrest.Matcher<String> startsWith(String prefix) {
    return org.hamcrest.Matchers.startsWith(prefix);
  }

  /** Проверяет расшифрованную строку запроса (query) обращения к /api/customers. */
  private static RequestMatcher query(java.util.function.Consumer<String> check) {
    return request -> {
      assertThat(request.getURI().toString()).startsWith(CUSTOMERS);
      check.accept(request.getURI().getQuery());
    };
  }
}
