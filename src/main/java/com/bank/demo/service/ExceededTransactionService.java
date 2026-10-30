package com.bank.demo.service;

import java.util.List;

import com.bank.demo.dto.ExceededTransactionResponse;

public interface ExceededTransactionService {
 
    List<ExceededTransactionResponse> getExceededTransactions(
        String accountFrom, String category);
}
