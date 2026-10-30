package com.bank.demo.service.impl;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.bank.demo.dto.ExceededTransactionResponse;
import com.bank.demo.dto.ExpenseCategory;
import com.bank.demo.repository.TransactionAssessmentRepository;
import com.bank.demo.repository.projection.ExceededTransactionProjection;
import com.bank.demo.service.ExceededTransactionService;

import lombok.AllArgsConstructor;

@Service 
@AllArgsConstructor 
public class ExceededTransactionServiceImpl implements ExceededTransactionService {

    private final TransactionAssessmentRepository transactionAssessmentRepository;

    @Override
    public List<ExceededTransactionResponse> getExceededTransactions(
        String accountFrom, String category) {
        
        List<ExceededTransactionProjection> exceededTransactionProjections = 
        transactionAssessmentRepository.findExceededTransactions(accountFrom, category);

        List<ExceededTransactionResponse> exceededTransactionResponses = new ArrayList<>();

        for(ExceededTransactionProjection projection : exceededTransactionProjections){

            ExceededTransactionResponse response = new ExceededTransactionResponse();

            response.setAccountFrom(projection.getAccountFrom());
            response.setAccountTo(projection.getAccountTo());
            response.setCurrencyShortname(projection.getCurrencyShortname());
            response.setDatetime(projection.getDatetime().atOffset(ZoneOffset.UTC));             
            response.setExpenseCategory(ExpenseCategory.valueOf(
                projection.getExpenseCategory().toUpperCase(Locale.ROOT)));
            response.setLimitCurrencyShortname(projection.getLimitCurrencyShortname());
            response.setSum(projection.getSum());
            response.setLimitSum(projection.getLimitSum());

            if (projection.getLimitDate() != null){
                response.setLimitDate(projection.getLimitDate().atOffset(ZoneOffset.UTC));
            } else {
                response.setLimitDate(null);
            }

            exceededTransactionResponses.add(response);
        }                
        return exceededTransactionResponses;
    }    
}
