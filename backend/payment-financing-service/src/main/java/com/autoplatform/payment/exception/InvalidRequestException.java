package com.autoplatform.payment.exception;

/** Business-rule validation failure that's the caller's fault. */
public class InvalidRequestException extends RuntimeException {
    public InvalidRequestException(String message) {
        super(message);
    }
}
