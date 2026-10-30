package com.bank.demo.repository.projection;

import java.math.BigDecimal;
import java.time.Instant;

public interface ExceededTransactionProjection {
    String getAccountTo();
    String getCurrencyShortname();
    BigDecimal getSum();
    Instant getDatetime();
    Instant getLimitDate();
    String getLimitCurrencyShortname();
    String getAccountFrom();
    BigDecimal getLimitSum();
    String getExpenseCategory();
}
