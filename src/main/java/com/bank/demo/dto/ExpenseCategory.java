package com.bank.demo.dto;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ExpenseCategory{

    @JsonProperty("product")
    PRODUCT,
    
    @JsonProperty("service")
    SERVICE;

    @JsonValue
    public String toLower() {
        return this.name().toLowerCase(Locale.ROOT);
    }
}