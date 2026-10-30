package com.bank.demo.service;

import java.math.BigDecimal;

import com.bank.demo.entity.ExpenseTransaction;

public interface TransactionAssessmentService {
    
        void assess(ExpenseTransaction transaction, BigDecimal amountUsd, Long rateId);

}
