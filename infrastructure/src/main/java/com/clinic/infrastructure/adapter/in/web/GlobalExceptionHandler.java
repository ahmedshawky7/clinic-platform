package com.clinic.infrastructure.adapter.in.web;

import com.clinic.domain.exception.InvalidAppointmentTransitionException;
import com.clinic.domain.exception.SubscriptionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("detail", ex.getMessage(), "error", ex.getMessage()));
    }

    @ExceptionHandler({IllegalStateException.class, InvalidAppointmentTransitionException.class})
    public ResponseEntity<Map<String, String>> handleConflict(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("detail", ex.getMessage(), "error", ex.getMessage()));
    }

    @ExceptionHandler(SubscriptionException.class)
    public ResponseEntity<Map<String, String>> handleSubscription(SubscriptionException ex) {
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(Map.of("detail", ex.getMessage(), "error", ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("detail", ex.getMessage(), "error", ex.getMessage()));
    }
}