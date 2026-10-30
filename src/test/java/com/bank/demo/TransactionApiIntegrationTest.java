package com.bank.demo;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.bank.demo.service.TransactionProcessingService;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TransactionApiIntegrationTest {

    private static final WireMockServer FX_SERVER = new WireMockServer(0);
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionProcessingService transactionProcessingService;

    @BeforeAll
    static void startFxServer() {
        FX_SERVER.start();
        FX_SERVER.stubFor(get(urlPathEqualTo("/v1/rate"))
                .withQueryParam("from", equalTo("EUR"))
                .withQueryParam("to", equalTo("USD"))
                .withQueryParam("date", equalTo("2026-01-03"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "data": {"from": "EUR", "to": "USD", "rate": 2.0},
                                  "meta": {"last_updated_at": "2026-01-03T00:00:00Z"}
                                }
                                """)));
    }

    @AfterAll
    static void stopFxServer() {
        FX_SERVER.stop();
    }

    @DynamicPropertySource
    static void configureFxApi(DynamicPropertyRegistry properties) {
        properties.add("integration.fxapi.base-url", FX_SERVER::baseUrl);
        properties.add("integration.fxapi.api-key", () -> "test-key");
    }

    @Test
    void acceptsJanuaryTransactionsAndReturnsTheExceededOneWithAppliedLimit() throws Exception {
        String accountFrom = UUID.randomUUID().toString()
                .replaceAll("\\D", "").substring(0, 10);
        jdbcTemplate.update("INSERT INTO customer_account (account_from) VALUES (?)", accountFrom);
        Long accountId = jdbcTemplate.queryForObject(
                "SELECT id FROM customer_account WHERE account_from = ?", Long.class, accountFrom);
        jdbcTemplate.update(
                "INSERT INTO budget_scope (account_id, category) VALUES (?, 'product')", accountId);
        Long scopeId = jdbcTemplate.queryForObject(
                "SELECT id FROM budget_scope WHERE account_id = ? AND category = 'product'",
                Long.class, accountId);
        jdbcTemplate.update("""
                INSERT INTO spending_limit (budget_scope_id, amount_usd, effective_at)
                VALUES (?, 100.00, TIMESTAMPTZ '2026-01-01T00:00:00Z')
                """, scopeId);

        HttpResponse<String> januaryThird = postTransaction(accountFrom,
                "2026-01-03T12:00:00Z", "40.00");
        HttpResponse<String> januaryThirteenth = postTransaction(accountFrom,
                "2026-01-13T12:00:00Z", "20.00");

        assertEquals(202, januaryThird.statusCode(), januaryThird.body());
        assertEquals(202, januaryThirteenth.statusCode(), januaryThirteenth.body());
        assertTrue(januaryThird.body().contains("ASSESSED"), januaryThird.body());
        assertTrue(januaryThirteenth.body().contains("ASSESSED"), januaryThirteenth.body());

        HttpRequest exceededRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port
                        + "/api/transactions/exceeded?account_from=" + accountFrom
                        + "&expense_category=product"))
                .GET()
                .build();
        HttpResponse<String> exceeded = HTTP.send(exceededRequest,
                HttpResponse.BodyHandlers.ofString());

        assertEquals(200, exceeded.statusCode(), exceeded.body());
        assertTrue(exceeded.body().contains("2026-01-13T12:00:00Z"), exceeded.body());
        assertTrue(exceeded.body().contains("\"sum\":20.00"), exceeded.body());
        assertTrue(exceeded.body().contains("\"limitSum\":100.00"), exceeded.body());
        assertTrue(exceeded.body().contains("\"limitCurrencyShortname\":\"USD\""),
                exceeded.body());
        assertEquals(1, jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM transaction_assessment ta
                JOIN expense_transaction et ON et.id = ta.transaction_id
                JOIN budget_scope bs ON bs.id = et.budget_scope_id
                JOIN customer_account ca ON ca.id = bs.account_id
                WHERE ca.account_from = ? AND ta.limit_exceeded
                """, Integer.class, accountFrom));
        FX_SERVER.verify(1, getRequestedFor(urlPathEqualTo("/v1/rate"))
                .withQueryParam("date", equalTo("2026-01-03")));
    }

    @Test
    void supportsKztAndRubAndReusesTheActualPriorCloseDateFromProvider() throws Exception {
        FX_SERVER.stubFor(get(urlPathEqualTo("/v1/rate"))
                .withQueryParam("from", equalTo("KZT"))
                .withQueryParam("to", equalTo("USD"))
                .withQueryParam("date", equalTo("2025-12-11"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "data": {"from": "KZT", "to": "USD", "rate": 0.002},
                                  "meta": {"last_updated_at": "2025-12-09T23:59:59Z"}
                                }
                                """)));
        FX_SERVER.stubFor(get(urlPathEqualTo("/v1/rate"))
                .withQueryParam("from", equalTo("RUB"))
                .withQueryParam("to", equalTo("USD"))
                .withQueryParam("date", equalTo("2025-12-13"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "data": {"from": "RUB", "to": "USD", "rate": 0.01},
                                  "meta": {"last_updated_at": "2025-12-13T23:59:59Z"}
                                }
                                """)));

        String kztAccount = randomAccount();
        HttpResponse<String> firstKztTransaction = postTransaction(
                kztAccount, "KZT", "2025-12-11T12:00:00Z", "1000.00");
        assertEquals(202, firstKztTransaction.statusCode(), firstKztTransaction.body());
        assertTrue(firstKztTransaction.body().contains("ASSESSED"), firstKztTransaction.body());

        assertEquals(LocalDate.of(2025, 12, 9), jdbcTemplate.queryForObject("""
                SELECT rate_date FROM currency_rate
                WHERE base_currency = 'KZT' AND quote_currency = 'USD'
                AND rate_date = DATE '2025-12-09'
                """, LocalDate.class));
        assertEquals(0, new BigDecimal("0.002").compareTo(jdbcTemplate.queryForObject("""
                SELECT usd_per_unit FROM currency_rate
                WHERE base_currency = 'KZT' AND quote_currency = 'USD'
                AND rate_date = DATE '2025-12-09'
                """, BigDecimal.class)));

        HttpResponse<String> secondKztTransaction = postTransaction(
                randomAccount(), "KZT", "2025-12-11T14:00:00Z", "500.00");
        assertEquals(202, secondKztTransaction.statusCode(), secondKztTransaction.body());
        assertTrue(secondKztTransaction.body().contains("ASSESSED"), secondKztTransaction.body());
        FX_SERVER.verify(1, getRequestedFor(urlPathEqualTo("/v1/rate"))
                .withQueryParam("from", equalTo("KZT"))
                .withQueryParam("date", equalTo("2025-12-11")));

        HttpResponse<String> rubTransaction = postTransaction(
                randomAccount(), "RUB", "2025-12-13T12:00:00Z", "100.00");
        assertEquals(202, rubTransaction.statusCode(), rubTransaction.body());
        assertTrue(rubTransaction.body().contains("ASSESSED"), rubTransaction.body());
        assertEquals(LocalDate.of(2025, 12, 13), jdbcTemplate.queryForObject("""
                SELECT rate_date FROM currency_rate WHERE base_currency = 'RUB' AND quote_currency = 'USD'
                AND rate_date = DATE '2025-12-13'
                """, LocalDate.class));
        FX_SERVER.verify(1, getRequestedFor(urlPathEqualTo("/v1/rate"))
                .withQueryParam("from", equalTo("RUB"))
                .withQueryParam("to", equalTo("USD"))
                .withQueryParam("date", equalTo("2025-12-13")));
    }

    @Test
    void retainsTransactionWhenRateProviderFails() throws Exception {
        FX_SERVER.stubFor(get(urlPathEqualTo("/v1/rate"))
                .withQueryParam("from", equalTo("JPY"))
                .withQueryParam("to", equalTo("USD"))
                .withQueryParam("date", equalTo("2026-01-08"))
                .willReturn(aResponse().withStatus(503)));

        String accountFrom = randomAccount();
        HttpResponse<String> response = postTransaction(
                accountFrom, "JPY", "2026-01-08T12:00:00Z", "1000.00");

        assertEquals(202, response.statusCode(), response.body());
        assertTrue(response.body().contains("RETRYABLE_ERROR"), response.body());
        assertEquals(1, jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM expense_transaction et
                JOIN budget_scope bs ON bs.id = et.budget_scope_id
                JOIN customer_account ca ON ca.id = bs.account_id
                WHERE ca.account_from = ? AND et.processing_status = 'RETRYABLE_ERROR'
                AND et.next_retry_at IS NOT NULL
                """, Integer.class, accountFrom));
    }

    @Test
    void retriesSavedTransactionAfterRateProviderRecovers() throws Exception {
        String scenario = "KZT rate provider recovery for " + UUID.randomUUID();
        FX_SERVER.stubFor(get(urlPathEqualTo("/v1/rate"))
                .withQueryParam("from", equalTo("KZT"))
                .withQueryParam("to", equalTo("USD"))
                .withQueryParam("date", equalTo("2026-01-07"))
                .inScenario(scenario)
                .whenScenarioStateIs(com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED)
                .willReturn(aResponse().withStatus(503))
                .willSetStateTo("recovered"));
        FX_SERVER.stubFor(get(urlPathEqualTo("/v1/rate"))
                .withQueryParam("from", equalTo("KZT"))
                .withQueryParam("to", equalTo("USD"))
                .withQueryParam("date", equalTo("2026-01-07"))
                .inScenario(scenario)
                .whenScenarioStateIs("recovered")
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "data": {"from": "KZT", "to": "USD", "rate": 0.002},
                                  "meta": {"last_updated_at": "2026-01-07T23:59:59Z"}
                                }
                                """)));

        String accountFrom = randomAccount();
        HttpResponse<String> initialResponse = postTransaction(
                accountFrom, "KZT", "2026-01-07T12:00:00Z", "1000.00");
        assertEquals(202, initialResponse.statusCode(), initialResponse.body());
        assertTrue(initialResponse.body().contains("RETRYABLE_ERROR"), initialResponse.body());

        jdbcTemplate.update("""
                UPDATE expense_transaction et
                SET next_retry_at = CURRENT_TIMESTAMP - INTERVAL '1 second'
                FROM budget_scope bs
                JOIN customer_account ca ON ca.id = bs.account_id
                WHERE et.budget_scope_id = bs.id AND ca.account_from = ?
                """, accountFrom);
        transactionProcessingService.retryFailedRates();

        assertEquals(1, jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM expense_transaction et
                JOIN budget_scope bs ON bs.id = et.budget_scope_id
                JOIN customer_account ca ON ca.id = bs.account_id
                JOIN transaction_assessment ta ON ta.transaction_id = et.id
                WHERE ca.account_from = ? AND et.processing_status = 'ASSESSED'
                AND et.next_retry_at IS NULL AND ta.rate_id IS NOT NULL
                """, Integer.class, accountFrom));
        FX_SERVER.verify(2, getRequestedFor(urlPathEqualTo("/v1/rate"))
                .withQueryParam("from", equalTo("KZT"))
                .withQueryParam("date", equalTo("2026-01-07")));
    }

    @Test
    void concurrentHttpTransactionsShareTheSameLimitBalance() throws Exception {
        String accountFrom = randomAccount();
        Long accountId = jdbcTemplate.queryForObject(
                "SELECT id FROM customer_account WHERE account_from = ?", Long.class, accountFrom);
        Long scopeId = jdbcTemplate.queryForObject("""
                INSERT INTO budget_scope (account_id, category) VALUES (?, 'product')
                RETURNING id
                """, Long.class, accountId);
        jdbcTemplate.update("""
                INSERT INTO spending_limit (budget_scope_id, amount_usd, effective_at)
                VALUES (?, 100.00, TIMESTAMPTZ '2026-01-01T00:00:00Z')
                """, scopeId);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<HttpResponse<String>> first = executor.submit(() -> {
                ready.countDown();
                start.await();
                return postTransaction(accountFrom, "USD", "2026-01-13T12:00:00Z", "60.00");
            });
            Future<HttpResponse<String>> second = executor.submit(() -> {
                ready.countDown();
                start.await();
                return postTransaction(accountFrom, "USD", "2026-01-13T12:00:01Z", "60.00");
            });

            ready.await();
            start.countDown();
            HttpResponse<String> firstResponse = first.get();
            HttpResponse<String> secondResponse = second.get();
            assertEquals(202, firstResponse.statusCode(), firstResponse.body());
            assertEquals(202, secondResponse.statusCode(), secondResponse.body());
        }

        assertEquals(1, jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM transaction_assessment ta
                JOIN expense_transaction et ON et.id = ta.transaction_id
                JOIN budget_scope bs ON bs.id = et.budget_scope_id
                JOIN customer_account ca ON ca.id = bs.account_id
                WHERE ca.account_from = ? AND ta.limit_exceeded
                """, Integer.class, accountFrom));
        assertEquals(120, jdbcTemplate.queryForObject("""
                SELECT ms.spent_usd FROM monthly_spend ms
                JOIN budget_scope bs ON bs.id = ms.budget_scope_id
                JOIN customer_account ca ON ca.id = bs.account_id
                WHERE ca.account_from = ? AND bs.category = 'product'
                AND ms.month_start = DATE '2026-01-01'
                """, BigDecimal.class, accountFrom).intValue());
    }

    private HttpResponse<String> postTransaction(String accountFrom, String datetime, String sum)
            throws Exception {
        return postTransaction(accountFrom, "EUR", datetime, sum);
    }

    private String randomAccount() {
        String accountFrom = UUID.randomUUID().toString()
                .replaceAll("\\D", "").substring(0, 10);
        jdbcTemplate.update("INSERT INTO customer_account (account_from) VALUES (?)", accountFrom);
        return accountFrom;
    }

    private HttpResponse<String> postTransaction(
            String accountFrom, String currency, String datetime, String sum) throws Exception {
        String body = """
                {
                  "accountFrom": "%s",
                  "accountTo": "2222222222",
                  "currencyShortname": "%s",
                  "sum": %s,
                  "expenseCategory": "product",
                  "datetime": "%s"
                }
                """.formatted(accountFrom, currency, sum, datetime);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/transactions"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return HTTP.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
