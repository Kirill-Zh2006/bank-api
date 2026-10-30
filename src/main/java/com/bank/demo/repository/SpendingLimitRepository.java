package com.bank.demo.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bank.demo.entity.SpendingLimit;

public interface SpendingLimitRepository extends JpaRepository<SpendingLimit, Long>{

    Optional<SpendingLimit> 
    findFirstByBudgetScopeIdAndEffectiveAtLessThanEqualOrderByEffectiveAtDesc(
        Long budgetScopeId, Instant effectiveAt
    );

    List<SpendingLimit> findAllByBudgetScopeIdInOrderByEffectiveAtDesc(
        Collection<Long> budgetScopeIds
    );
}
