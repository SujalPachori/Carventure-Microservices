package com.carventure.car.exception;

import com.carventure.car.exception.CarIdNotFoundException;
import com.carventure.car.exception.UserNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(CarIdNotFoundException.class)
    public ResponseEntity<Map<String,String>> notFound(CarIdNotFoundException e) {
        return ResponseEntity.status(404).body(Map.of("message", e.getMessage()));
    }
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String,String>> userNotFound(UserNotFoundException e) {
        return ResponseEntity.status(404).body(Map.of("message", e.getMessage()));
    }
}