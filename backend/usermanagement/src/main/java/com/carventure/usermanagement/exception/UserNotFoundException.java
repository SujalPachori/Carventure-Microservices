package com.carventure.usermanagement.exception;

/**
 * ADDED (new file). UserServiceImpl threw bare RuntimeException, which
 * Spring turns into a 500. A missing user is a 404 — see the advice class
 * that maps it.
 */
public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String message) {
        super(message);
    }
}
