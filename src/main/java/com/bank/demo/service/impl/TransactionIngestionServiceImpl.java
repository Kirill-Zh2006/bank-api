package com.bank.demo.service.impl;

import java.time.Instant;
import java.time.Clock;

import org.springframework.stereotype.Service;

import com.bank.demo.dto.TransactionProcessingStatus;
import com.bank.demo.dto.TransactionRequest;
import com.bank.demo.entity.BudgetScope;
import com.bank.demo.entity.CustomerAccount;
import com.bank.demo.entity.ExpenseTransaction;
import com.bank.demo.repository.BudgetScopeRepository;
import com.bank.demo.repository.CustomerAccountRepository;
import com.bank.demo.repository.ExpenseTransactionRepository;
import com.bank.demo.service.TransactionIngestionService;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;

@Service 
@AllArgsConstructor 
public class TransactionIngestionServiceImpl implements TransactionIngestionService{

    private final ExpenseTransactionRepository expenseTransactionRepository;

    private final CustomerAccountRepository customerAccountRepository;

    private final BudgetScopeRepository budgetScopeRepository;

    private final Clock clock;

    @Transactional 
    @Override
    public ExpenseTransaction savePendingTransaction(TransactionRequest request) {

        Instant receivedAt = clock.instant();

        CustomerAccount customerAccount = customerAccountRepository.findByAccountFrom(
            request.getAccountFrom())
            .orElseGet(() -> {
                CustomerAccount account = new CustomerAccount();
                account.setAccountFrom(request.getAccountFrom());
                account.setCreatedAt(receivedAt);

                return customerAccountRepository.save(account);
            });
        
        BudgetScope budgetScope = budgetScopeRepository.findByAccountIdAndCategory(
            customerAccount.getId(), request.getExpenseCategory().toLower())
                .orElseGet(() -> {
                    BudgetScope scope = new BudgetScope();
                    scope.setAccountId(customerAccount.getId());
                    scope.setCategory(request.getExpenseCategory().toLower());

                    return budgetScopeRepository.save(scope);
                });

        ExpenseTransaction expenseTransaction = new ExpenseTransaction();
        
        expenseTransaction.setBudgetScopeId(budgetScope.getId());
        expenseTransaction.setAccountTo(request.getAccountTo());
        expenseTransaction.setCurrencyCode(request.getCurrencyShortname());
        expenseTransaction.setAmountOriginal(request.getSum());
        expenseTransaction.setOccurredAt(request.getDatetime().toInstant());
        expenseTransaction.setReceivedAt(receivedAt);
        expenseTransaction.setProcessingStatus(TransactionProcessingStatus.PENDING_RATE.name());
    
        return expenseTransactionRepository.save(expenseTransaction);
    }
    
}
