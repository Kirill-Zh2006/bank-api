package com.bank.demo.service.impl;

import java.time.Instant;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.bank.demo.dto.ExpenseCategory;
import com.bank.demo.dto.LimitResponse;
import com.bank.demo.dto.SetLimitRequest;
import com.bank.demo.entity.BudgetScope;
import com.bank.demo.entity.CustomerAccount;
import com.bank.demo.entity.SpendingLimit;
import com.bank.demo.repository.BudgetScopeRepository;
import com.bank.demo.repository.CustomerAccountRepository;
import com.bank.demo.repository.SpendingLimitRepository;
import com.bank.demo.service.LimitService;

import lombok.AllArgsConstructor;

@Service 
@AllArgsConstructor 
public class LimitServiceImpl implements LimitService{

    private final CustomerAccountRepository customerAccountRepository;

    private final BudgetScopeRepository budgetScopeRepository;

    private final SpendingLimitRepository spendingLimitRepository;

    private final Clock clock;

    @Override
    public LimitResponse setLimit(SetLimitRequest request) {

        Instant now = clock.instant();

        CustomerAccount account = customerAccountRepository
            .findByAccountFrom(request.getAccountFrom())
            .orElseGet(() -> {
                CustomerAccount newAccount = new CustomerAccount();
                newAccount.setAccountFrom(request.getAccountFrom());
                newAccount.setCreatedAt(now);
                return customerAccountRepository.save(newAccount);
            });

        String category = request.getExpenseCategory().toLower();
        
        BudgetScope scope = budgetScopeRepository
            .findByAccountIdAndCategory(account.getId(), category)
            .orElseGet(() -> {
                BudgetScope newScope = new BudgetScope();
                newScope.setAccountId(account.getId());
                newScope.setCategory(category);
                return budgetScopeRepository.save(newScope);
            });

        SpendingLimit limit = new SpendingLimit();
        limit.setBudgetScopeId(scope.getId());
        limit.setAmountUsd(request.getLimitSum());
        limit.setEffectiveAt(now);
        
        spendingLimitRepository.save(limit);

        LimitResponse response = new LimitResponse();
        response.setAccountFrom(account.getAccountFrom());
        response.setExpenseCategory(ExpenseCategory.valueOf(request.getExpenseCategory()
            .name().toUpperCase(Locale.ROOT)));
        response.setLimitSum(request.getLimitSum());
        response.setLimitDatetime(limit.getEffectiveAt().atOffset(ZoneOffset.UTC));
        response.setLimitCurrencyShortname("USD");

        return response;
    }

    @Override
    public List<LimitResponse> getLimits(String accountFrom) {

        return customerAccountRepository.findByAccountFrom(accountFrom)
                .map(account -> {
                    List<BudgetScope> scopes = 
                            budgetScopeRepository.findAllByAccountId(account.getId());
                    
                    if(scopes.isEmpty()){
                        return List.<LimitResponse>of();
                    }

                    Map<Long, BudgetScope> scopesById = scopes.stream()
                            .collect(Collectors.toMap(BudgetScope::getId, scope -> scope));
                    
                    return spendingLimitRepository
                            .findAllByBudgetScopeIdInOrderByEffectiveAtDesc(scopesById.keySet())
                            .stream()
                            .map(limit -> {
                                BudgetScope scope = scopesById.get(limit.getBudgetScopeId());

                                LimitResponse response = new LimitResponse();
                                response.setAccountFrom(account.getAccountFrom());
                                response.setExpenseCategory(ExpenseCategory.valueOf(
                                        scope.getCategory().toUpperCase(Locale.ROOT)));
                                response.setLimitSum(limit.getAmountUsd());
                                response.setLimitDatetime(limit.getEffectiveAt()
                                        .atOffset(ZoneOffset.UTC));
                                response.setLimitCurrencyShortname("USD");
                                return response;
                            })
                            .toList();
                })
                .orElseGet(List::of);
    }
    
}
