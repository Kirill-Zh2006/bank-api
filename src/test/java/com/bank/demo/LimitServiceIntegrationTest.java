package com.bank.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "integration.fxapi.api-key=test-key")
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, LimitServiceIntegrationTest.FixedClockConfiguration.class})
class LimitServiceIntegrationTest {

    private static final Instant INITIAL_TIME = Instant.parse("2026-01-01T00:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MutableClock clock;

    @BeforeEach
    void resetClock() {
        clock.setInstant(INITIAL_TIME);
    }

    @Test
    void returnsLimitHistoryForAllAccountCategoriesNewestFirst() throws Exception {
        String accountFrom = randomAccount();
        Long accountId = createAccount(accountFrom);
        Long productScopeId = createScope(accountId, "product");
        Long serviceScopeId = createScope(accountId, "service");
        createLimit(productScopeId, "100.00", "2026-01-01T00:00:00Z");
        createLimit(productScopeId, "150.00", "2026-01-15T00:00:00Z");
        createLimit(serviceScopeId, "75.00", "2026-01-12T00:00:00Z");

        mockMvc.perform(get("/api/limits").param("account_from", accountFrom))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(3)))
                .andExpect(jsonPath("$[0].accountFrom").value(accountFrom))
                .andExpect(jsonPath("$[0].expenseCategory").value("product"))
                .andExpect(jsonPath("$[0].limitSum").value(150.00))
                .andExpect(jsonPath("$[0].limitDatetime").value("2026-01-15T00:00:00Z"))
                .andExpect(jsonPath("$[0].limitCurrencyShortname").value("USD"))
                .andExpect(jsonPath("$[1].expenseCategory").value("service"))
                .andExpect(jsonPath("$[1].limitSum").value(75.00))
                .andExpect(jsonPath("$[1].limitDatetime").value("2026-01-12T00:00:00Z"))
                .andExpect(jsonPath("$[2].expenseCategory").value("product"))
                .andExpect(jsonPath("$[2].limitSum").value(100.00))
                .andExpect(jsonPath("$[2].limitDatetime").value("2026-01-01T00:00:00Z"));
    }

    @Test
    void returnsEmptyListWhenAccountHasNoLimits() throws Exception {
        mockMvc.perform(get("/api/limits").param("account_from", randomAccount()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void recordsLimitChangesAsHistoryThroughHttp() throws Exception {
        String accountFrom = randomAccount();

        mockMvc.perform(post("/api/limits")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(limitRequest(accountFrom, "100.00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.limitDatetime").value("2026-01-01T00:00:00Z"));

        clock.setInstant(Instant.parse("2026-01-15T00:00:00Z"));
        mockMvc.perform(post("/api/limits")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(limitRequest(accountFrom, "150.00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.limitDatetime").value("2026-01-15T00:00:00Z"));

        mockMvc.perform(get("/api/limits").param("account_from", accountFrom))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$[0].accountFrom").value(accountFrom))
                .andExpect(jsonPath("$[0].expenseCategory").value("product"))
                .andExpect(jsonPath("$[0].limitSum").value(150.00))
                .andExpect(jsonPath("$[0].limitDatetime").value("2026-01-15T00:00:00Z"))
                .andExpect(jsonPath("$[1].limitSum").value(100.00))
                .andExpect(jsonPath("$[1].limitDatetime").value("2026-01-01T00:00:00Z"));

        assertEquals(1, jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM budget_scope bs
                JOIN customer_account ca ON ca.id = bs.account_id
                WHERE ca.account_from = ? AND bs.category = 'product'
                """, Integer.class, accountFrom));
    }

    private String limitRequest(String accountFrom, String amount) {
        return """
                {
                  "accountFrom": "%s",
                  "expenseCategory": "product",
                  "limitSum": %s
                }
                """.formatted(accountFrom, amount);
    }

    private Long createAccount(String accountFrom) {
        jdbcTemplate.update("INSERT INTO customer_account (account_from) VALUES (?)", accountFrom);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM customer_account WHERE account_from = ?", Long.class, accountFrom);
    }

    private Long createScope(Long accountId, String category) {
        jdbcTemplate.update(
                "INSERT INTO budget_scope (account_id, category) VALUES (?, ?)",
                accountId, category);
        return jdbcTemplate.queryForObject("""
                SELECT id FROM budget_scope WHERE account_id = ? AND category = ?
                """, Long.class, accountId, category);
    }

    private void createLimit(Long scopeId, String amount, String effectiveAt) {
        jdbcTemplate.update("""
                INSERT INTO spending_limit (budget_scope_id, amount_usd, effective_at)
                VALUES (?, ?, ?::timestamptz)
                """, scopeId, new BigDecimal(amount), effectiveAt);
    }

    private String randomAccount() {
        return UUID.randomUUID().toString().replaceAll("\\D", "").substring(0, 10);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedClockConfiguration {

        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock(INITIAL_TIME, ZoneOffset.UTC);
        }
    }

    static class MutableClock extends Clock {

        private final AtomicReference<Instant> currentInstant;
        private final ZoneId zone;

        MutableClock(Instant initialInstant, ZoneId zone) {
            this(new AtomicReference<>(initialInstant), zone);
        }

        private MutableClock(AtomicReference<Instant> currentInstant, ZoneId zone) {
            this.currentInstant = currentInstant;
            this.zone = zone;
        }

        void setInstant(Instant instant) {
            currentInstant.set(instant);
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(currentInstant, zone);
        }

        @Override
        public Instant instant() {
            return currentInstant.get();
        }
    }
}
