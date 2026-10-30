package com.bank.demo.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bank.demo.entity.MonthlySpend;

public interface MonthlySpendRepository extends JpaRepository<MonthlySpend, Long>{
    
    Optional<MonthlySpend> findByBudgetScopeIdAndMonthStart(
        Long budgetScopeId, LocalDate monthStart
    );
}
