package com.autoplatform.payment.exception;

/** Thrown when Car Service can't be reached (circuit open / all retries exhausted). */
public class CarServiceUnavailableException extends RuntimeException {
    public CarServiceUnavailableException(String message) {
        super(message);
    }
}
