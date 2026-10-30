package com.bank.demo.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bank.demo.entity.ExpenseTransaction;

public interface ExpenseTransactionRepository extends JpaRepository<ExpenseTransaction, Long>{
    
    List<ExpenseTransaction>
    findTop5ByProcessingStatusAndNextRetryAtLessThanEqualOrderByNextRetryAtAscReceivedAtAsc(
        String processingStatus,
        Instant now);
} 
