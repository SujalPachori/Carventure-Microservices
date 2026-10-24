package com.autoplatform.payment.exception;

/** Thrown when a Stripe webhook payload fails signature verification - always reject it, never process the body. */
public class InvalidWebhookSignatureException extends RuntimeException {
    public InvalidWebhookSignatureException(String message) {
        super(message);
    }
}
