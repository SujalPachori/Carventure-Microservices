package com.autoplatform.payment.exception;

/** Thrown when an order tries to complete a sale for a car another order already sold (race lost). */
public class CarAlreadySoldException extends RuntimeException {
    public CarAlreadySoldException(String message) {
        super(message);
    }
}
