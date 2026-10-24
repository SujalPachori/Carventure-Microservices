package com.autoplatform.payment.exception;

/** Thrown when an action is attempted against an Order/Payment that is not in a compatible status. */
public class InvalidStateTransitionException extends RuntimeException {
    public InvalidStateTransitionException(String message) {
        super(message);
    }
}
