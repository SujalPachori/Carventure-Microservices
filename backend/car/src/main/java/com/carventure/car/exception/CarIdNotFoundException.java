package com.carventure.car.exception;

public class CarIdNotFoundException extends RuntimeException {
    public CarIdNotFoundException(String message) {
        super(message);
    }
}
