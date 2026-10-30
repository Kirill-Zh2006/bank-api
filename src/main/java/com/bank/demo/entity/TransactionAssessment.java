package com.bank.demo.entity;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "transaction_assessment")
public class TransactionAssessment {
    
    @Id 
    @Column(name = "transaction_id")
    private Long transactionId;

    @Column(name = "amount_usd")
    private BigDecimal amountUsd;

    @Column(name = "rate_id")
    private Long rateId;

    @Column(name = "monthly_spend_id")
    private Long monthlySpendId;

    @Column(name = "limit_id")
    private Long limitId;

    @Column(name = "applied_limit_usd")
    private BigDecimal appliedLimitUsd;

    @Column(name = "limit_source")
    private String limitSource;

    @Column(name = "limit_exceeded")
    private Boolean limitExceeded;

    @Column(name = "assessed_at")
    private Instant assessedAt;
}
