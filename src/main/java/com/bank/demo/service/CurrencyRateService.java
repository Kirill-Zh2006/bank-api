package com.bank.demo.service;

import java.time.LocalDate;

import com.bank.demo.entity.CurrencyRate;

public interface CurrencyRateService {
    
    CurrencyRate getRate(String currency, LocalDate date);
}
