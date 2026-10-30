package com.bank.demo.entity;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "expense_transaction")
public class ExpenseTransaction {
    
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "budget_scope_id")
    private Long budgetScopeId;

    @Column(name = "account_to")
    private String accountTo;

    @Column(name = "currency_code")
    private String currencyCode;

    @Column(name = "amount_original")
    private BigDecimal amountOriginal;

    @Column(name = "occurred_at")
    private Instant occurredAt;

    @Column(name = "received_at")
    private Instant receivedAt;

    @Column(name = "processing_status")
    private String processingStatus;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;
    
}
