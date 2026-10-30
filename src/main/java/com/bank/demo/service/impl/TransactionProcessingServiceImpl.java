package com.bank.demo.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import com.bank.demo.dto.TransactionProcessingStatus;
import com.bank.demo.dto.TransactionRequest;
import com.bank.demo.entity.CurrencyRate;
import com.bank.demo.entity.ExpenseTransaction;
import com.bank.demo.repository.ExpenseTransactionRepository;
import com.bank.demo.service.CurrencyRateService;
import com.bank.demo.service.TransactionAssessmentService;
import com.bank.demo.service.TransactionIngestionService;
import com.bank.demo.service.TransactionProcessingService;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j 
@Service 
@AllArgsConstructor 
public class TransactionProcessingServiceImpl implements TransactionProcessingService {

    private final TransactionIngestionService transactionIngestionService;

    private final CurrencyRateService currencyRateService;

    private final ExpenseTransactionRepository expenseTransactionRepository;
    
    private final TransactionAssessmentService transactionAssessmentService;

    private final Clock clock;

    @Override
    public ExpenseTransaction processTransaction(TransactionRequest request) {

        ExpenseTransaction expenseTransaction = 
        transactionIngestionService.savePendingTransaction(request);

        BigDecimal amountUsd = expenseTransaction.getAmountOriginal();
        Long rateId = null;

        if(!"USD".equals(expenseTransaction.getCurrencyCode())){

            LocalDate rateDate = expenseTransaction.getOccurredAt()
                    .atZone(ZoneOffset.UTC)
                    .toLocalDate();

            CurrencyRate rate;
            try {
                rate = currencyRateService.getRate(
                    expenseTransaction.getCurrencyCode(), rateDate);

            } catch (RestClientException | IllegalStateException e) {
                expenseTransaction.setProcessingStatus(
                    TransactionProcessingStatus.RETRYABLE_ERROR.name());
                expenseTransaction.setNextRetryAt(clock.instant().plusSeconds(60));

                log.warn("Rate lookup failed for transaction {}",
                    expenseTransaction.getId(), e);

                return expenseTransactionRepository.save(expenseTransaction);
            }

            rateId = rate.getId();
            amountUsd = amountUsd.multiply(rate.getUsdPerUnit()).setScale(
                2, RoundingMode.HALF_UP);
        } 

        transactionAssessmentService.assess(expenseTransaction, amountUsd, rateId);

        return expenseTransaction;
    }

    @Override
    @Scheduled(fixedDelayString = "${app.retry.fixed-delay-ms:60000}")
    public void retryFailedRates() {

        Instant now = clock.instant();

        List<ExpenseTransaction> transactions = expenseTransactionRepository
        .findTop5ByProcessingStatusAndNextRetryAtLessThanEqualOrderByNextRetryAtAscReceivedAtAsc(
            TransactionProcessingStatus.RETRYABLE_ERROR.name(), now);

        for(ExpenseTransaction transaction : transactions){    

            CurrencyRate rate;
            try {
                rate = currencyRateService.getRate(
                        transaction.getCurrencyCode(),
                        transaction.getOccurredAt()
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate());

            } catch (RestClientException | IllegalStateException e) {
                transaction.setProcessingStatus(
                    TransactionProcessingStatus.RETRYABLE_ERROR.name());
                transaction.setNextRetryAt(clock.instant().plusSeconds(60));

                log.warn("Rate lookup failed for transaction {}",
                    transaction.getId(), e);
                
                expenseTransactionRepository.save(transaction);
                
                continue;
            }
            
            BigDecimal amountUsd = transaction.getAmountOriginal()
                    .multiply(rate.getUsdPerUnit())
                    .setScale(2, RoundingMode.HALF_UP);
            
            transactionAssessmentService.assess(transaction, amountUsd, rate.getId());
        }   
    }    
}
