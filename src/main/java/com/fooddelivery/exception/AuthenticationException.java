package com.fooddelivery.exception;

/**
 * Thrown when login fails because of invalid credentials.
 */
public class AuthenticationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param message description of the failure
     */
    public AuthenticationException(String message) {
        super(message);
    }
}
