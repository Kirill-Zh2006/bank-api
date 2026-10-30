package com.bank.demo.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bank.demo.dto.LimitResponse;
import com.bank.demo.dto.ApiErrorResponse;
import com.bank.demo.dto.SetLimitRequest;
import com.bank.demo.service.LimitService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;

@Validated
@RestController
@RequestMapping("/api/limits")
@AllArgsConstructor
@Tag(name = "Limit") 
public class LimitController {

        private final LimitService limitService;

    @Operation(
            summary = "Set limits",
            description = "Sets new limits"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Limits successfully set",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = LimitResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation error or incorrect JSON",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    @PostMapping
    public ResponseEntity<LimitResponse> setLimit(
            @Valid @RequestBody SetLimitRequest request) {

        LimitResponse response = limitService.setLimit(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Get limits",
            description = "Get all limits"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Limits successfully get",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                        array = @ArraySchema(
                            schema = @Schema(implementation = LimitResponse.class)))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation error or invalid query parameter",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    @GetMapping
    public ResponseEntity<List<LimitResponse>> getLimits(
            @RequestParam(name = "account_from")
            @Pattern(regexp = "[0-9]{10}") String accountFrom) {

        return ResponseEntity.ok().body(limitService.getLimits(accountFrom));
    }
}
