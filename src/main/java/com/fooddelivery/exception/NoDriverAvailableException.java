package com.fooddelivery.exception;

public class NoDriverAvailableException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public NoDriverAvailableException(String message) {
        super(message);
    }
}
