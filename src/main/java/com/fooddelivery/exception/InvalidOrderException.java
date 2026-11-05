package com.fooddelivery.exception;

/**
 * Thrown when an order's content is invalid: no items, an unavailable item,
 * a bad quantity, or a closed restaurant.
 */
public class InvalidOrderException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param message description of why the order is invalid
     */
    public InvalidOrderException(String message) {
        super(message);
    }
}
