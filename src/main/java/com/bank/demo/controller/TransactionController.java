package com.bank.demo.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.bank.demo.dto.ExceededTransactionResponse;
import com.bank.demo.dto.ApiErrorResponse;
import com.bank.demo.dto.TransactionAcceptedResponse;
import com.bank.demo.dto.TransactionProcessingStatus;
import com.bank.demo.dto.TransactionRequest;
import com.bank.demo.entity.ExpenseTransaction;
import com.bank.demo.service.ExceededTransactionService;
import com.bank.demo.service.TransactionProcessingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@Validated 
@RestController
@AllArgsConstructor
@Tag(name = "Transaction")
public class TransactionController {

    private final TransactionProcessingService transactionProcessingService;

    private final ExceededTransactionService exceededTransactionService;
    
    @Operation(
            summary = "Accept transaction"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "202",
                    description = "Transaction successfully accepted",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TransactionAcceptedResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation error or incorrect JSON",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    @PostMapping("/api/transactions")
    public ResponseEntity<TransactionAcceptedResponse> acceptTransaction(
            @Valid @RequestBody TransactionRequest transactionRequest) {

        ExpenseTransaction savedTransaction = 
        transactionProcessingService.processTransaction(transactionRequest);

        TransactionAcceptedResponse tResponse = new TransactionAcceptedResponse();
        tResponse.setTransactionId(savedTransaction.getId());
        tResponse.setProcessingStatus(TransactionProcessingStatus.valueOf(
                savedTransaction.getProcessingStatus()));

        return ResponseEntity.accepted().body(tResponse);
    }

    @Operation(
            summary = "Get exceeded transactions"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Transaction successfully gets",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                        array = @ArraySchema(
                            schema = @Schema(implementation = ExceededTransactionResponse.class))
                            )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation error or invalid query parameter",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    @GetMapping("/api/transactions/exceeded")
    public ResponseEntity<List<ExceededTransactionResponse>> getExceededTransaction(
            @RequestParam(name = "account_from")
            @Pattern(regexp = "[0-9]{10}") String accountFrom,
            @RequestParam(name = "expense_category")
            @Pattern(regexp = "product|service") String expenseCategory){

        return ResponseEntity.ok(exceededTransactionService.getExceededTransactions(
                accountFrom, expenseCategory));
    }
}
