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
@Table(name = "spending_limit")
public class SpendingLimit {
    
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "budget_scope_id")
    private Long budgetScopeId;

    @Column(name = "amount_usd")
    private BigDecimal amountUsd;

    @Column(name = "effective_at")
    private Instant effectiveAt;
}
