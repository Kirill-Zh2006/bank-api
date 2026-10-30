package com.bank.demo.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Validated
@ConfigurationProperties(prefix = "integration.fxapi")
public record FxApiProperties(
    @NotBlank String baseUrl,
    @NotBlank String apiKey,
    @NotNull Client client
) {
    public record Client(
        @NotNull Duration connectTimeout,
        @NotNull Duration readTimeout
    ) {}
}
