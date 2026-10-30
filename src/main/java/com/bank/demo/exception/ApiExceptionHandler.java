package com.bank.demo.exception;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            ConstraintViolationException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<ProblemDetail> handleRequestException(Exception ex) {
        Map<String, String> errors = new HashMap<>();

        if (ex instanceof MethodArgumentNotValidException methodEx) {
            methodEx.getBindingResult().getFieldErrors().forEach(error ->
                    errors.put(error.getField(), error.getDefaultMessage()));
        } else if (ex instanceof ConstraintViolationException constraintEx) {
            constraintEx.getConstraintViolations().forEach(violation -> {
                String propertyPath = violation.getPropertyPath().toString();
                String fieldName =
                        propertyPath.substring(propertyPath.lastIndexOf('.') + 1);

                errors.put(fieldName, violation.getMessage());
            });
        }

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setType(URI.create("about:blank"));
        problem.setTitle("Invalid request");
        problem.setDetail("The request contains invalid or unreadable values");
        problem.setProperty("errors", errors);

        return ResponseEntity.badRequest().body(problem);
    }
}
