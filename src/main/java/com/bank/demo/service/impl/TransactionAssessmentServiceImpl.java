package com.bank.demo.service.impl;

import com.bank.demo.repository.TransactionAssessmentRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.bank.demo.dto.TransactionProcessingStatus;
import com.bank.demo.entity.ExpenseTransaction;
import com.bank.demo.entity.MonthlySpend;
import com.bank.demo.entity.SpendingLimit;
import com.bank.demo.entity.TransactionAssessment;
import com.bank.demo.repository.BudgetScopeRepository;
import com.bank.demo.repository.ExpenseTransactionRepository;
import com.bank.demo.repository.MonthlySpendRepository;
import com.bank.demo.repository.SpendingLimitRepository;
import com.bank.demo.service.TransactionAssessmentService;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;

@Service 
@AllArgsConstructor 
public class TransactionAssessmentServiceImpl implements TransactionAssessmentService{

    private final TransactionAssessmentRepository transactionAssessmentRepository;

    private final BudgetScopeRepository budgetScopeRepository;

    private final MonthlySpendRepository monthlySpendRepository;

    private final SpendingLimitRepository spendingLimitRepository;

    private final ExpenseTransactionRepository expenseTransactionRepository;

    private final Clock clock;

    @Override
    @Transactional 
    public void assess(ExpenseTransaction transaction, BigDecimal amountUsd, Long rateId) {
        
        budgetScopeRepository.findByIdForUpdate(transaction.getBudgetScopeId())
        .orElseThrow(() -> new IllegalStateException(
        "Budget scope not found: " + transaction.getBudgetScopeId()));

        Instant assessedAt = clock.instant();

        LocalDate monthStart = transaction.getOccurredAt()
        .atZone(ZoneOffset.UTC)
        .toLocalDate()
        .withDayOfMonth(1);

        Optional<MonthlySpend> monthlySpend = monthlySpendRepository
        .findByBudgetScopeIdAndMonthStart(
                transaction.getBudgetScopeId(), monthStart);

        BigDecimal spentBeforeUsd = monthlySpend
            .map(MonthlySpend::getSpentUsd)
            .orElse(BigDecimal.ZERO);

        Optional<SpendingLimit> effectiveLimit =
        spendingLimitRepository
                .findFirstByBudgetScopeIdAndEffectiveAtLessThanEqualOrderByEffectiveAtDesc(
                        transaction.getBudgetScopeId(),
                        transaction.getOccurredAt());

        BigDecimal appliedLimitUsd = effectiveLimit
            .map(SpendingLimit::getAmountUsd)
            .orElse(new BigDecimal("1000.00"));
        
        String limitSource = effectiveLimit.isPresent() ? "CUSTOM" : "DEFAULT";
        Long limitId = effectiveLimit.map(SpendingLimit::getId).orElse(null);

        boolean limitExceeded =
        spentBeforeUsd.add(amountUsd).compareTo(appliedLimitUsd) > 0;

        MonthlySpend spend = monthlySpend.orElseGet(() -> {
            MonthlySpend newSpend = new MonthlySpend();
            newSpend.setBudgetScopeId(transaction.getBudgetScopeId());
            newSpend.setMonthStart(monthStart);
            newSpend.setSpentUsd(BigDecimal.ZERO);
            
            return newSpend;
        });

        spend.setSpentUsd(spentBeforeUsd.add(amountUsd));
        spend.setUpdatedAt(assessedAt);
        
        MonthlySpend savedSpend = monthlySpendRepository.save(spend);

        TransactionAssessment transactionAssessment = new TransactionAssessment();
        transactionAssessment.setTransactionId(transaction.getId());
        transactionAssessment.setAmountUsd(amountUsd);
        transactionAssessment.setMonthlySpendId(savedSpend.getId());
        transactionAssessment.setLimitExceeded(limitExceeded);
        transactionAssessment.setAppliedLimitUsd(appliedLimitUsd);
        transactionAssessment.setLimitSource(limitSource);
        transactionAssessment.setAssessedAt(assessedAt);
        transactionAssessment.setLimitId(limitId);
        transactionAssessment.setRateId(rateId);

        transactionAssessmentRepository.save(transactionAssessment);

        transaction.setProcessingStatus(TransactionProcessingStatus.ASSESSED.name());
        transaction.setNextRetryAt(null);
        expenseTransactionRepository.save(transaction);
    }
    
}
