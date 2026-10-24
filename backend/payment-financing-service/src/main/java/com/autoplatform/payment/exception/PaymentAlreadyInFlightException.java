package com.autoplatform.payment.exception;

public class PaymentAlreadyInFlightException extends RuntimeException {
    public PaymentAlreadyInFlightException(String message) {
        super(message);
    }
}