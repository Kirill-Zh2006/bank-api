package com.bank.demo.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Data;

@Data 
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ExceededTransactionResponse {

    private String accountFrom;

    private String accountTo;
 
    private String currencyShortname;

    private BigDecimal sum;

    private ExpenseCategory expenseCategory;

    private OffsetDateTime datetime;

    private OffsetDateTime limitDate;

    private BigDecimal limitSum;

    private String limitCurrencyShortname;
}
