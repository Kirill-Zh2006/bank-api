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
public class LimitResponse {

    private String accountFrom;
    
    private ExpenseCategory expenseCategory;

    private BigDecimal limitSum;
    
    private OffsetDateTime limitDatetime;

    private String limitCurrencyShortname;
}
