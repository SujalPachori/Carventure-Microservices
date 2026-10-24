package com.carventure.offer.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OfferNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(OfferNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(InvalidOfferStateException.class)
    public ResponseEntity<Map<String, String>> handleState(InvalidOfferStateException ex) {
        return build(HttpStatus.CONFLICT, "INVALID_STATE", ex.getMessage());
    }

    @ExceptionHandler(CarAlreadySoldException.class)
    public ResponseEntity<Map<String, String>> handleSold(CarAlreadySoldException ex) {
        return build(HttpStatus.CONFLICT, "CAR_ALREADY_SOLD", ex.getMessage());
    }

    @ExceptionHandler(NotAllowedException.class)
    public ResponseEntity<Map<String, String>> handleNotAllowed(NotAllowedException ex) {
        return build(HttpStatus.FORBIDDEN, "NOT_ALLOWED", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(e -> e.getField() + " " + e.getDefaultMessage())
                .orElse("Invalid request");
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }

    private ResponseEntity<Map<String, String>> build(HttpStatus status, String code, String message) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("status", code);
        body.put("message", message);
        return new ResponseEntity<>(body, status);
    }
}
