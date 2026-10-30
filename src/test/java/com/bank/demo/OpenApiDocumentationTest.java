package com.bank.demo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "integration.fxapi.api-key=test-key")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OpenApiDocumentationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void documentsApiErrorResponseForBadRequests() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())

                .andExpect(jsonPath(
                        "$.components.schemas.ApiErrorResponse.properties.type").exists())
                .andExpect(jsonPath(
                        "$.components.schemas.ApiErrorResponse.properties.title").exists())
                .andExpect(jsonPath(
                        "$.components.schemas.ApiErrorResponse.properties.status").exists())
                .andExpect(jsonPath(
                        "$.components.schemas.ApiErrorResponse.properties.detail").exists())
                .andExpect(jsonPath(
                        "$.components.schemas.ApiErrorResponse.properties.instance").exists())
                .andExpect(jsonPath(
                        "$.components.schemas.ApiErrorResponse.properties.errors").exists())

                .andExpect(jsonPath(
                        "$.paths['/api/transactions'].post.responses['400']"
                                + ".content['application/problem+json'].schema['$ref']")
                        .value("#/components/schemas/ApiErrorResponse"))
                .andExpect(jsonPath(
                        "$.paths['/api/transactions/exceeded'].get.responses['400']"
                                + ".content['application/problem+json'].schema['$ref']")
                        .value("#/components/schemas/ApiErrorResponse"))
                .andExpect(jsonPath(
                        "$.paths['/api/limits'].post.responses['400']"
                                + ".content['application/problem+json'].schema['$ref']")
                        .value("#/components/schemas/ApiErrorResponse"))
                .andExpect(jsonPath(
                        "$.paths['/api/limits'].get.responses['400']"
                                + ".content['application/problem+json'].schema['$ref']")
                        .value("#/components/schemas/ApiErrorResponse"))
                // POST /api/transactions 202
                .andExpect(jsonPath(
                        "$.paths['/api/transactions'].post.requestBody"
                                + ".content['application/json'].schema['$ref']")
                        .value("#/components/schemas/TransactionRequest"))
                .andExpect(jsonPath(
                        "$.paths['/api/transactions'].post.responses['202']"
                                + ".content['application/json'].schema['$ref']")
                        .value("#/components/schemas/TransactionAcceptedResponse"))

                // GET /api/transactions/exceeded 200
                .andExpect(jsonPath(
                        "$.paths['/api/transactions/exceeded'].get.responses['200']"
                                + ".content['application/json'].schema.items['$ref']")
                        .value("#/components/schemas/ExceededTransactionResponse"))

                // POST /api/limits 201
                .andExpect(jsonPath(
                        "$.paths['/api/limits'].post.requestBody"
                                + ".content['application/json'].schema['$ref']")
                        .value("#/components/schemas/SetLimitRequest"))
                .andExpect(jsonPath(
                        "$.paths['/api/limits'].post.responses['201']"
                                + ".content['application/json'].schema['$ref']")
                        .value("#/components/schemas/LimitResponse"))

                // GET /api/limits 200
                .andExpect(jsonPath(
                        "$.paths['/api/limits'].get.responses['200']"
                                + ".content['application/json'].schema.items['$ref']")
                        .value("#/components/schemas/LimitResponse"));
    }
}
