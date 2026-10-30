package com.bank.demo.client;

import com.bank.demo.dto.FxRateResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.math.BigDecimal;
import java.time.LocalDate;

@Component
public class FxApiAdapter {

    private final RestClient restClient;

    public FxApiAdapter(RestClient fxapiRestClient) {
        this.restClient = fxapiRestClient;
    }

    public FxRateResponse getRateResponse(String currency, LocalDate date) {
        FxRateResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/rate")
                        .queryParam("from", currency)
                        .queryParam("to", "USD")
                        .queryParam("date", date.toString())
                        .build())
                .retrieve()
                .body(FxRateResponse.class);

        if (response == null || response.data() == null) {
            throw new IllegalStateException("Provider returns empty answer for currency: " + currency);
        }

        BigDecimal rate = response.data().rate();
        if(rate == null){
            throw new IllegalStateException("Provider returned no rate for currency: " + currency);
        }

        if(response.meta() == null || response.meta().lastUpdatedAt() == null){
            throw new IllegalStateException("Provider returned no meta for currency: " + currency);
        }

        return response;
    }
}
