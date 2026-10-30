package com.bank.demo;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bank.demo.controller.LimitController;
import com.bank.demo.dto.ExpenseCategory;
import com.bank.demo.dto.LimitResponse;
import com.bank.demo.dto.SetLimitRequest;
import com.bank.demo.service.LimitService;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LimitController.class)
class LimitControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LimitService limitService;

    @Test
    void createsLimitAndReturnsCreatedResponse() throws Exception {
        LimitResponse response = new LimitResponse();
        response.setAccountFrom("1234567890");
        response.setExpenseCategory(ExpenseCategory.PRODUCT);
        response.setLimitSum(new BigDecimal("250.00"));
        response.setLimitDatetime(OffsetDateTime.parse("2026-01-10T12:00:00Z"));
        response.setLimitCurrencyShortname("USD");
        when(limitService.setLimit(any(SetLimitRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/limits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accountFrom": "1234567890",
                                  "expenseCategory": "product",
                                  "limitSum": 250.00
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accountFrom").value("1234567890"))
                .andExpect(jsonPath("$.expenseCategory").value("product"))
                .andExpect(jsonPath("$.limitSum").value(250.00))
                .andExpect(jsonPath("$.limitDatetime").value("2026-01-10T12:00:00Z"))
                .andExpect(jsonPath("$.limitCurrencyShortname").value("USD"));

        verify(limitService).setLimit(any(SetLimitRequest.class));
    }

    @Test
    void acceptsSnakeCaseLimitRequestAndKeepsCamelCaseResponse() throws Exception {
        LimitResponse response = new LimitResponse();
        response.setAccountFrom("1234567890");
        response.setExpenseCategory(ExpenseCategory.PRODUCT);
        response.setLimitSum(new BigDecimal("250.00"));
        response.setLimitDatetime(OffsetDateTime.parse("2026-01-10T12:00:00Z"));
        response.setLimitCurrencyShortname("USD");
        when(limitService.setLimit(any(SetLimitRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/limits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "account_from": "1234567890",
                                  "expense_category": "product",
                                  "limit_sum": 250.00
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountFrom").value("1234567890"))
                .andExpect(jsonPath("$.limitSum").value(250.00));

        verify(limitService).setLimit(any(SetLimitRequest.class));
    }

    @Test
    void rejectsLimitWithInvalidAccountFrom() throws Exception {
        mockMvc.perform(post("/api/limits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "account_from": "invalid",
                                  "expense_category": "product",
                                  "limit_sum": 100.00
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Invalid request"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.accountFrom").exists());
    }

    @Test
    void rejectsLimitsQueryWithInvalidAccountFrom() throws Exception {
        mockMvc.perform(get("/api/limits")
                        .param("account_from", "invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Invalid request"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.accountFrom").exists());
    }
}
