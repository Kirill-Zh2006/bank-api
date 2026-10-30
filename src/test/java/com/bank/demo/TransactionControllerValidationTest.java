package com.bank.demo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.hamcrest.Matchers.containsString;

import com.bank.demo.controller.TransactionController;
import com.bank.demo.service.ExceededTransactionService;
import com.bank.demo.service.TransactionProcessingService;
import com.bank.demo.dto.TransactionProcessingStatus;
import com.bank.demo.dto.TransactionRequest;
import com.bank.demo.entity.ExpenseTransaction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TransactionController.class)
class TransactionControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionProcessingService transactionProcessingService;

    @MockitoBean
    private ExceededTransactionService exceededTransactionService;

    @Test
    void rejectsTransactionWithInvalidAccountFrom() throws Exception {
        mockMvc.perform(post("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "account_from": "invalid",
                          "account_to": "1234567890",
                          "currency_shortname": "USD",
                          "sum": 10.00,
                          "expense_category": "product",
                          "datetime": "2026-01-13T12:00:00Z"
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Invalid request"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.accountFrom").exists());

        verifyNoInteractions(transactionProcessingService);
    }

    @Test
    void acceptsSnakeCaseTransactionFieldsAndReturnsCamelCaseResponse() throws Exception {
        ExpenseTransaction transaction = new ExpenseTransaction();
        transaction.setId(42L);
        transaction.setProcessingStatus(TransactionProcessingStatus.ASSESSED.name());
        when(transactionProcessingService.processTransaction(any())).thenReturn(transaction);

        var result = mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "account_from": "1234567890",
                                  "account_to": "0987654321",
                                  "currency_shortname": "USD",
                                  "sum": 10.00,
                                  "expense_category": "product",
                                  "datetime": "2026-01-13T12:00:00Z"
                                }
                                """))
                .andReturn();
        org.junit.jupiter.api.Assertions.assertEquals(202, result.getResponse().getStatus(),
                result.getResponse().getContentAsString());
        jsonPath("$.transactionId").value(42).match(result);
        jsonPath("$.processingStatus").value("ASSESSED").match(result);

        verify(transactionProcessingService).processTransaction(argThat((TransactionRequest request) ->
                "1234567890".equals(request.getAccountFrom())
                        && "USD".equals(request.getCurrencyShortname())
                        && request.getSum().compareTo(new java.math.BigDecimal("10.00")) == 0));
    }

    @Test
    void rejectsExceededTransactionsWithInvalidAccountFrom() throws Exception {
        mockMvc.perform(get("/api/transactions/exceeded")
                .param("account_from", "invalid")
                .param("expense_category", "product"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Invalid request"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.accountFrom").exists());

        verifyNoInteractions(exceededTransactionService);
    }

    @Test
    void returnsProblemDetailWithEmptyErrorsForMalformedJson() throws Exception {
        mockMvc.perform(post("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"account_from\":"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value("Invalid request"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value(
                        "The request contains invalid or unreadable values"))
                .andExpect(content().string(containsString("\"errors\":{}")));

        verifyNoInteractions(transactionProcessingService);
    }
}
