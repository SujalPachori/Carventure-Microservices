package com.autoplatform.payment.exception;

/** Thrown when Stripe rejects a request or returns an unexpected error (e.g. StripeException wrapped). */
public class PaymentProcessingException extends RuntimeException {
    public PaymentProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
    public PaymentProcessingException(String message) {
        super(message);
    }
}
