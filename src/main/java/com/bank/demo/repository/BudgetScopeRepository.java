package com.bank.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bank.demo.entity.BudgetScope;

import jakarta.persistence.LockModeType;

public interface BudgetScopeRepository extends JpaRepository<BudgetScope, Long>{

    Optional<BudgetScope> findByAccountIdAndCategory(Long accountId, String category);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from BudgetScope b where b.id = :id")
    Optional<BudgetScope> findByIdForUpdate(@Param("id") Long id);

    List<BudgetScope> findAllByAccountId(Long accountId);
}
