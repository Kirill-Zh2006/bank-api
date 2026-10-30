package com.bank.demo;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.bank.demo.dto.ExceededTransactionResponse;
import com.bank.demo.dto.ExpenseCategory;
import com.bank.demo.entity.BudgetScope;
import com.bank.demo.entity.CustomerAccount;
import com.bank.demo.entity.ExpenseTransaction;
import com.bank.demo.entity.MonthlySpend;
import com.bank.demo.entity.SpendingLimit;
import com.bank.demo.entity.TransactionAssessment;
import com.bank.demo.repository.BudgetScopeRepository;
import com.bank.demo.repository.CustomerAccountRepository;
import com.bank.demo.repository.ExpenseTransactionRepository;
import com.bank.demo.repository.MonthlySpendRepository;
import com.bank.demo.repository.SpendingLimitRepository;
import com.bank.demo.repository.TransactionAssessmentRepository;
import com.bank.demo.service.ExceededTransactionService;
import com.bank.demo.service.TransactionAssessmentService;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class TransactionAssessmentServiceTest {

    @Autowired
    private CustomerAccountRepository customerAccountRepository;

    @Autowired
    private BudgetScopeRepository budgetScopeRepository;

    @Autowired
    private SpendingLimitRepository spendingLimitRepository;

    @Autowired
    private ExpenseTransactionRepository expenseTransactionRepository;

    @Autowired
    private MonthlySpendRepository monthlySpendRepository;

    @Autowired
    private TransactionAssessmentRepository transactionAssessmentRepository;

    @Autowired
    private TransactionAssessmentService transactionAssessmentService;

    @Autowired
    private ExceededTransactionService exceededTransactionService;

    @Test
    void limitIsNotExceededWhenMonthlySpendEqualsLimit() {
        BudgetScope scope = createScope("1111111111");
        createLimit(scope, "100.00", "2026-01-01T00:00:00Z");
        ExpenseTransaction transaction = createTransaction(
                scope, "2026-01-13T12:00:00Z", "100.00");

        TransactionAssessment assessment = assess(transaction, "100.00");

        assertFalse(assessment.getLimitExceeded());
    }

    @Test
    void transactionUsesLimitEffectiveAtItsEventTime() {
        BudgetScope scope = createScope("1111111111");
        createLimit(scope, "100.00", "2026-01-01T00:00:00Z");

        ExpenseTransaction beforeChange = createTransaction(
                scope, "2026-01-05T12:00:00Z", "40.00");
        TransactionAssessment firstAssessment = assess(beforeChange, "40.00");

        SpendingLimit newLimit = createLimit(
                scope, "50.00", "2026-01-10T00:00:00Z");
        ExpenseTransaction afterChange = createTransaction(
                scope, "2026-01-13T12:00:00Z", "20.00");
        TransactionAssessment secondAssessment = assess(afterChange, "20.00");

        assertFalse(firstAssessment.getLimitExceeded());
        assertEquals(newLimit.getId(), secondAssessment.getLimitId());
        assertTrue(secondAssessment.getLimitExceeded());
    }

    @Test
    void monthlySpendStartsAtZeroInNewUtcMonth() {
        BudgetScope scope = createScope("1111111111");
        createLimit(scope, "100.00", "2026-01-01T00:00:00Z");

        ExpenseTransaction januaryTransaction = createTransaction(
                scope, "2026-01-31T23:00:00Z", "80.00");
        TransactionAssessment januaryAssessment = assess(
                januaryTransaction, "80.00");

        ExpenseTransaction februaryTransaction = createTransaction(
                scope, "2026-02-01T00:00:00Z", "30.00");
        TransactionAssessment februaryAssessment = assess(
                februaryTransaction, "30.00");

        assertFalse(januaryAssessment.getLimitExceeded());
        assertFalse(februaryAssessment.getLimitExceeded());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentTransactionsShareTheSameMonthlyLimitBalance() throws Exception {
        String accountFrom = String.format(
                "%010d", ThreadLocalRandom.current().nextLong(10_000_000_000L));
        BudgetScope scope = createScope(accountFrom);
        createLimit(scope, "100.00", "2026-01-01T00:00:00Z");

        ExpenseTransaction firstTransaction = createTransaction(
                scope, "2026-01-13T12:00:00Z", "60.00");
        ExpenseTransaction secondTransaction = createTransaction(
                scope, "2026-01-13T12:00:00Z", "60.00");

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            List<Future<?>> assessments = List.of(
                    executor.submit(() -> assessConcurrently(
                            firstTransaction, ready, start)),
                    executor.submit(() -> assessConcurrently(
                            secondTransaction, ready, start)));

            assertTrue(ready.await(10, TimeUnit.SECONDS),
                    "Both workers should be ready before assessment starts");
            start.countDown();

            for (Future<?> assessment : assessments) {
                assessment.get(10, TimeUnit.SECONDS);
            }
        } finally {
            start.countDown();
            executor.shutdownNow();
        }

        List<TransactionAssessment> assessments = List.of(
                transactionAssessmentRepository.findById(firstTransaction.getId())
                        .orElseThrow(),
                transactionAssessmentRepository.findById(secondTransaction.getId())
                        .orElseThrow());

        assertEquals(1, assessments.stream()
                .filter(TransactionAssessment::getLimitExceeded)
                .count());

        MonthlySpend monthlySpend = monthlySpendRepository
                .findByBudgetScopeIdAndMonthStart(
                        scope.getId(), LocalDate.of(2026, 1, 1))
                .orElseThrow();
        assertEquals(0, new BigDecimal("120.00")
                .compareTo(monthlySpend.getSpentUsd()));
    }

    @Test
    void returnsOnlyExceededTransactionsWithAppliedLimit() {
        String accountFrom = "3333333333";
        BudgetScope scope = createScope(accountFrom);
        createLimit(scope, "100.00", "2026-01-01T00:00:00Z");

        ExpenseTransaction withinLimit = createTransaction(
                scope, "2026-01-05T12:00:00Z", "40.00");
        assess(withinLimit, "40.00");

        ExpenseTransaction exceeded = createTransaction(
                scope, "2026-01-06T12:00:00Z", "70.00");
        assess(exceeded, "70.00");

        List<ExceededTransactionResponse> responses = exceededTransactionService.getExceededTransactions(
                accountFrom, "product");

        assertEquals(1, responses.size());

        ExceededTransactionResponse response = responses.get(0);
        assertEquals(accountFrom, response.getAccountFrom());
        assertEquals("2222222222", response.getAccountTo());
        assertEquals("USD", response.getCurrencyShortname());
        assertEquals(0, new BigDecimal("70.00").compareTo(response.getSum()));
        assertEquals(ExpenseCategory.PRODUCT, response.getExpenseCategory());
        assertEquals(
                Instant.parse("2026-01-06T12:00:00Z"),
                response.getDatetime().toInstant());
        assertEquals(
                Instant.parse("2026-01-01T00:00:00Z"),
                response.getLimitDate().toInstant());
        assertEquals(0, new BigDecimal("100.00").compareTo(response.getLimitSum()));
        assertEquals("USD", response.getLimitCurrencyShortname());
    }

    @Test
    void returnsExceededTransactionsWithLimitEffectiveAtEachTransaction() {
        String accountFrom = "6666666666";
        BudgetScope scope = createScope(accountFrom);
        createLimit(scope, "100.00", "2026-01-01T00:00:00Z");

        ExpenseTransaction firstExceeded = createTransaction(
                scope, "2026-01-05T12:00:00Z", "110.00");
        assess(firstExceeded, "110.00");

        createLimit(scope, "200.00", "2026-01-10T00:00:00Z");
        ExpenseTransaction withinNewLimit = createTransaction(
                scope, "2026-01-11T12:00:00Z", "10.00");
        assess(withinNewLimit, "10.00");

        createLimit(scope, "50.00", "2026-01-15T00:00:00Z");
        ExpenseTransaction exceededByCumulativeSpend = createTransaction(
                scope, "2026-01-16T12:00:00Z", "1.00");
        assess(exceededByCumulativeSpend, "1.00");

        List<ExceededTransactionResponse> responses = exceededTransactionService
                .getExceededTransactions(accountFrom, "product");

        assertEquals(2, responses.size());

        ExceededTransactionResponse latest = responses.get(0);
        assertEquals(
                Instant.parse("2026-01-16T12:00:00Z"),
                latest.getDatetime().toInstant());
        assertEquals(
                Instant.parse("2026-01-15T00:00:00Z"),
                latest.getLimitDate().toInstant());
        assertEquals(0, new BigDecimal("50.00").compareTo(latest.getLimitSum()));

        ExceededTransactionResponse historical = responses.get(1);
        assertEquals(
                Instant.parse("2026-01-05T12:00:00Z"),
                historical.getDatetime().toInstant());
        assertEquals(
                Instant.parse("2026-01-01T00:00:00Z"),
                historical.getLimitDate().toInstant());
        assertEquals(0, new BigDecimal("100.00").compareTo(historical.getLimitSum()));
    }

    @Test
    void queryDeterminesExceededTransactionsFromCumulativeSpend() {
        String accountFrom = "7777777777";
        BudgetScope scope = createScope(accountFrom);
        createLimit(scope, "100.00", "2026-01-01T00:00:00Z");

        ExpenseTransaction withinLimit = createTransaction(
                scope, "2026-01-05T12:00:00Z", "40.00");
        assess(withinLimit, "40.00");

        ExpenseTransaction exceedsByCumulativeSpend = createTransaction(
                scope, "2026-01-06T12:00:00Z", "70.00");
        assess(exceedsByCumulativeSpend, "70.00");

        TransactionAssessment assessment = transactionAssessmentRepository
                .findById(exceedsByCumulativeSpend.getId())
                .orElseThrow();
        assessment.setLimitExceeded(false);
        transactionAssessmentRepository.saveAndFlush(assessment);

        List<ExceededTransactionResponse> responses = exceededTransactionService
                .getExceededTransactions(accountFrom, "product");

        assertEquals(1, responses.size());
        assertEquals(
                Instant.parse("2026-01-06T12:00:00Z"),
                responses.get(0).getDatetime().toInstant());
    }

    @Test
    void returnsEmptyListWhenNoTransactionsExceeded() {
        String accountFrom = "4444444444";
        BudgetScope scope = createScope(accountFrom);
        createLimit(scope, "100.00", "2026-01-01T00:00:00Z");

        ExpenseTransaction transaction = createTransaction(
                scope, "2026-01-05T12:00:00Z", "50.00");
        assess(transaction, "50.00");

        List<ExceededTransactionResponse> responses = exceededTransactionService.getExceededTransactions(
                accountFrom, "product");

        assertTrue(responses.isEmpty());
    }

    @Test
    void returnsExceededTransactionWithDefaultLimitAndNoLimitDate() {
        String accountFrom = "5555555555";
        BudgetScope scope = createScope(accountFrom);

        ExpenseTransaction transaction = createTransaction(
                scope, "2026-01-13T12:00:00Z", "1001.00");
        TransactionAssessment assessment = assess(transaction, "1001.00");

        assertTrue(assessment.getLimitExceeded());

        List<ExceededTransactionResponse> responses = exceededTransactionService.getExceededTransactions(
                accountFrom, "product");

        assertEquals(1, responses.size());

        ExceededTransactionResponse response = responses.get(0);
        assertEquals(accountFrom, response.getAccountFrom());
        assertNull(response.getLimitDate());
        assertEquals(0, new BigDecimal("1000.00").compareTo(response.getLimitSum()));
        assertEquals("USD", response.getLimitCurrencyShortname());
    }

    private void assessConcurrently(
            ExpenseTransaction transaction,
            CountDownLatch ready,
            CountDownLatch start) {
        ready.countDown();
        try {
            if (!start.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting to start assessment");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Assessment worker was interrupted", e);
        }

        transactionAssessmentService.assess(
                transaction, new BigDecimal("60.00"), null);
    }

    private BudgetScope createScope(String accountFrom) {
        CustomerAccount account = new CustomerAccount();
        account.setAccountFrom(accountFrom);
        account.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        account = customerAccountRepository.save(account);

        BudgetScope scope = new BudgetScope();
        scope.setAccountId(account.getId());
        scope.setCategory("product");
        return budgetScopeRepository.save(scope);
    }

    private SpendingLimit createLimit(
            BudgetScope scope, String amount, String effectiveAt) {
        SpendingLimit limit = new SpendingLimit();
        limit.setBudgetScopeId(scope.getId());
        limit.setAmountUsd(new BigDecimal(amount));
        limit.setEffectiveAt(Instant.parse(effectiveAt));
        return spendingLimitRepository.save(limit);
    }

    private ExpenseTransaction createTransaction(
            BudgetScope scope, String occurredAt, String amount) {
        ExpenseTransaction transaction = new ExpenseTransaction();
        transaction.setBudgetScopeId(scope.getId());
        transaction.setAccountTo("2222222222");
        transaction.setCurrencyCode("USD");
        transaction.setAmountOriginal(new BigDecimal(amount));
        transaction.setOccurredAt(Instant.parse(occurredAt));
        transaction.setReceivedAt(Instant.parse(occurredAt).plusSeconds(60));
        transaction.setProcessingStatus("PENDING_RATE");
        return expenseTransactionRepository.save(transaction);
    }

    private TransactionAssessment assess(
            ExpenseTransaction transaction, String amountUsd) {
        transactionAssessmentService.assess(
                transaction, new BigDecimal(amountUsd), null);
        return transactionAssessmentRepository.findById(transaction.getId())
                .orElseThrow();
    }
}
