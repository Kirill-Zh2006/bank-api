package com.bank.demo.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(FxApiProperties.class)
public class FxApiConfig {

    @Bean
    public RestClient fxapiRestClient(FxApiProperties properties) {

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) properties.client().connectTimeout().toMillis());
        requestFactory.setReadTimeout((int) properties.client().readTimeout().toMillis());

        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .requestInterceptor((request, body, execution) -> {
                    request.getHeaders().add("apikey", properties.apiKey());
                    return execution.execute(request, body);
                })
                .build();
    }
}
