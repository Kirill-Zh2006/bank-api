package com.bank.demo.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "currency_rate")
public class CurrencyRate {
    
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id") 
    private Long id;

    @Column(name = "base_currency")
    private String baseCurrency;

    @Column(name = "quote_currency")
    private String quoteCurrency;

    @Column(name = "rate_date")
    private LocalDate rateDate;

    @Column(name = "usd_per_unit")
    private BigDecimal usdPerUnit;
    
    @Column(name = "provider")
    private String provider;

    @Column(name = "fetched_at")
    private Instant fetchedAt;
}
