package com.bank.demo.service.impl;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.springframework.stereotype.Service;

import com.bank.demo.client.FxApiAdapter;
import com.bank.demo.dto.FxRateResponse;
import com.bank.demo.entity.CurrencyRate;
import com.bank.demo.repository.CurrencyRateRepository;
import com.bank.demo.service.CurrencyRateService;

import lombok.AllArgsConstructor;

@Service 
@AllArgsConstructor 
public class CurrencyRateServiceImpl implements CurrencyRateService{

    private final CurrencyRateRepository currencyRateRepository;

    private final FxApiAdapter fxApiAdapter;

    private final Clock clock;

    @Override
    public CurrencyRate getRate(String currency, LocalDate date) {

        CurrencyRate rate = currencyRateRepository
        .findTopByBaseCurrencyAndQuoteCurrencyAndRateDateLessThanEqualOrderByRateDateDesc(
            currency, "USD", date
        ).orElseGet(() -> {
            FxRateResponse apiResponse = fxApiAdapter.getRateResponse(currency, date);

            CurrencyRate newRate = new CurrencyRate();

            newRate.setBaseCurrency(currency);
            newRate.setQuoteCurrency("USD");
            newRate.setRateDate(apiResponse.meta().lastUpdatedAt()
                                    .atZone(ZoneOffset.UTC)
                                    .toLocalDate());
            newRate.setUsdPerUnit(apiResponse.data().rate());
            newRate.setProvider("fxapi");
            newRate.setFetchedAt(clock.instant());

            return currencyRateRepository.save(newRate);
        });

        return rate;        
    }
}
