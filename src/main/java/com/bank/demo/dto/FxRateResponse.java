package com.bank.demo.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FxRateResponse(
    Data data,
    Meta meta
) {
    public record Data(
        String from,
        String to,
        BigDecimal rate
    ) {}

    public record Meta(
        @JsonProperty("last_updated_at") Instant lastUpdatedAt
    ) {}
}

