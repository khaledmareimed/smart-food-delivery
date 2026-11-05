package com.fooddelivery.domain;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OrderStatusTest {

    @Test
    void onlyStatesBeforePreparationAreCancellable() {
        assertTrue(OrderStatus.PENDING.isCancellable());
        assertTrue(OrderStatus.CONFIRMED.isCancellable());
        assertFalse(OrderStatus.PREPARING.isCancellable());
        assertFalse(OrderStatus.READY.isCancellable());
        assertFalse(OrderStatus.OUT_FOR_DELIVERY.isCancellable());
        assertFalse(OrderStatus.DELIVERED.isCancellable());
        assertFalse(OrderStatus.CANCELLED.isCancellable());
    }
}
