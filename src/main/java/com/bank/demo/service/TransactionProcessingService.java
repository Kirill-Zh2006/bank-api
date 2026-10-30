package com.bank.demo.service;

import com.bank.demo.dto.TransactionRequest;
import com.bank.demo.entity.ExpenseTransaction;

public interface TransactionProcessingService {
    
    ExpenseTransaction processTransaction(TransactionRequest request);

    void retryFailedRates();
}
