package com.blackout.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Maps low-level web failures onto tidy JSON envelopes the terminal console can render
 * as field reports, instead of leaking stack traces over the wire.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> invalidBody(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> fields.put(error.getField(), error.getDefaultMessage()));
        return Map.of("error", "TRANSMISSION REJECTED", "fields", fields);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> unreadableBody(HttpMessageNotReadableException ex) {
        return Map.of("error", "MALFORMED TRANSMISSION BODY");
    }

    /** Cipher engines throw IllegalArgumentException on malformed field input. */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> badFieldInput(IllegalArgumentException ex) {
        return Map.of("error", "TOOL REJECTED", "detail", String.valueOf(ex.getMessage()));
    }
}
