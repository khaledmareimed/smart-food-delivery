package com.fooddelivery.exception;

/**
 * Thrown when an order breaks a delivery policy: invalid address, destination
 * outside the restaurant's delivery zone, or total below the minimum order value.
 */
public class DeliveryPolicyException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param message description of the violated policy
     */
    public DeliveryPolicyException(String message) {
        super(message);
    }
}
