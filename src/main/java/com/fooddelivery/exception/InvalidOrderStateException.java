package com.fooddelivery.exception;

/**
 * Thrown when an operation is not allowed in the order's current status,
 * for example cancelling an order that is already being prepared.
 */
public class InvalidOrderStateException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param message description of the forbidden operation
     */
    public InvalidOrderStateException(String message) {
        super(message);
    }
}
