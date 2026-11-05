package com.fooddelivery.exception;

/**
 * Thrown when a requested entity (restaurant, order, driver, ...) does not exist.
 */
public class ResourceNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param message description of what was not found
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
