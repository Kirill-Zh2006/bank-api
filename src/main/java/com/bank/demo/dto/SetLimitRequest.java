package com.bank.demo.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data 
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SetLimitRequest {

    @NotNull
    @Pattern(regexp = "[0-9]{10}")
    @JsonAlias("account_from")
    private String accountFrom;

    @NotNull
    @JsonAlias("expense_category")
    private ExpenseCategory expenseCategory;

    @NotNull
    @Positive
    @Digits(integer = 17, fraction = 2)
    @JsonAlias("limit_sum")
    private BigDecimal limitSum;

}
