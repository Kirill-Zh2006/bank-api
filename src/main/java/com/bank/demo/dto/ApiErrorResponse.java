package com.bank.demo.dto;

import java.net.URI;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ApiErrorResponse", description = "Problem details returned for invalid API requests")
public class ApiErrorResponse {

    @Schema(description = "A URI identifying the problem type", example = "about:blank")
    private URI type;

    @Schema(description = "A short summary of the problem", example = "Invalid request")
    private String title;

    @Schema(description = "The HTTP status code", example = "400")
    private Integer status;

    @Schema(description = "A human-readable explanation of the problem")
    private String detail;

    @Schema(description = "A URI identifying this specific occurrence")
    private URI instance;

    @Schema(description = "Validation errors keyed by request field; empty for unreadable JSON")
    private Map<String, String> errors;

    public URI getType() {
        return type;
    }

    public void setType(URI type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public URI getInstance() {
        return instance;
    }

    public void setInstance(URI instance) {
        this.instance = instance;
    }

    public Map<String, String> getErrors() {
        return errors;
    }

    public void setErrors(Map<String, String> errors) {
        this.errors = errors;
    }
}
