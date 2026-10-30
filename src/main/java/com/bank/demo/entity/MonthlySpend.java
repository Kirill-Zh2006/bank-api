package com.bank.demo.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "monthly_spend")
public class MonthlySpend {
    
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "budget_scope_id")
    private Long budgetScopeId;

    @Column(name = "month_start")
    private LocalDate monthStart;

    @Column(name = "spent_usd")
    private BigDecimal spentUsd;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
