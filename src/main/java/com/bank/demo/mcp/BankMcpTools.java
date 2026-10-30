package com.bank.demo.mcp;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;

import com.bank.demo.dto.ExceededTransactionResponse;
import com.bank.demo.dto.LimitResponse;
import com.bank.demo.service.ExceededTransactionService;
import com.bank.demo.service.LimitService;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;

@Component
public class BankMcpTools {

    private final LimitService limitService;
    private final ExceededTransactionService exceededTransactionService;

    public BankMcpTools(
            LimitService limitService,
            ExceededTransactionService exceededTransactionService) {
        this.limitService = limitService;
        this.exceededTransactionService = exceededTransactionService;
    }

    @McpTool(
            name = "get_limits",
            description = "Get the limit history for a ten-digit account number.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true))
    public List<LimitResponse> getLimits(
            @McpToolParam(description = "Ten-digit source account number", required = true)
            String accountFrom) {
        validateAccount(accountFrom);
        return limitService.getLimits(accountFrom);
    }

    @McpTool(
            name = "get_exceeded_transactions",
            description = "List transactions that exceeded the applicable monthly limit.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true))
    public List<ExceededTransactionResponse> getExceededTransactions(
            @McpToolParam(description = "Ten-digit source account number", required = true)
            String accountFrom,
            @McpToolParam(description = "Expense category: product or service", required = true)
            String expenseCategory) {
        validateAccount(accountFrom);
        if (!"product".equals(expenseCategory) && !"service".equals(expenseCategory)) {
            throw new IllegalArgumentException("expenseCategory must be product or service");
        }
        return exceededTransactionService.getExceededTransactions(
                accountFrom, expenseCategory.toLowerCase(Locale.ROOT));
    }

    private static void validateAccount(String accountFrom) {
        if (accountFrom == null || !accountFrom.matches("[0-9]{10}")) {
            throw new IllegalArgumentException("accountFrom must contain exactly 10 digits");
        }
    }
}
