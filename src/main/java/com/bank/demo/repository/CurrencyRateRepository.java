package com.bank.demo.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.bank.demo.entity.CurrencyRate;

public interface CurrencyRateRepository extends JpaRepository<CurrencyRate, Long>{
 
    Optional<CurrencyRate> 
    findTopByBaseCurrencyAndQuoteCurrencyAndRateDateLessThanEqualOrderByRateDateDesc(
        String baseCurrency,
        String quoteCurrency,
        LocalDate rateDate 
    );
    
}
