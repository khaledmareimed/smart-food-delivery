package com.fooddelivery.domain;

/**
 * Lifecycle states of an {@link Order}.
 * The allowed transitions between them are enforced in Sprint 4 (US4.1).
 */
public enum OrderStatus {
    /** Order created and waiting for the restaurant. */
    PENDING(true),
    /** Order accepted by the restaurant. */
    CONFIRMED(true),
    /** The restaurant is preparing the order. */
    PREPARING(false),
    /** The order is ready for pickup. */
    READY(false),
    /** A driver is delivering the order. */
    OUT_FOR_DELIVERY(false),
    /** The order reached the customer. */
    DELIVERED(false),
    /** The order was cancelled. */
    CANCELLED(false);

    private final boolean cancellable;

    OrderStatus(boolean cancellable) {
        this.cancellable = cancellable;
    }

    /**
     * @return {@code true} if an order in this state has not yet entered
     *         preparation and may therefore still be cancelled
     */
    public boolean isCancellable() {
        return cancellable;
    }
}
