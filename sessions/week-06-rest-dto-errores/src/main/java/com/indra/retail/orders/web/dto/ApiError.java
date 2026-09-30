package com.indra.retail.orders.web.dto;

import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

public record ApiError(Instant timestamp, int status, List<String> errors) {

    public static ApiError of(HttpStatus status, List<String> errors) {
        return new ApiError(Instant.now(), status.value(), errors);
    }

    public static ApiError of(HttpStatus status, String error) {
        return of(status, List.of(error));
    }
}
