package com.fooddelivery.domain;

import static org.junit.jupiter.api.Assertions.*;

import com.fooddelivery.exception.InvalidOrderStateException;
import java.math.BigDecimal;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** US4.1 order lifecycle. */
class OrderLifecycleTest {

    Order order;

    @BeforeEach
    void setUp() {
        MenuItem m = new MenuItem("m1", "A", new BigDecimal("10.00"), true);
        order = new Order("ORD-1", new User("u1", "alice", "h", Role.CUSTOMER),
                new Restaurant("r1", "R", true), Collections.singletonList(new OrderItem(m, 2)),
                new DeliveryAddress("Main", new Location(1, 1)));
    }

    @Test
    void fullValidLifecycle() {
        order.changeStatus(OrderStatus.CONFIRMED);
        order.changeStatus(OrderStatus.PREPARING);
        order.changeStatus(OrderStatus.READY);
        order.changeStatus(OrderStatus.OUT_FOR_DELIVERY);
        order.changeStatus(OrderStatus.DELIVERED);
        assertEquals(OrderStatus.DELIVERED, order.getStatus());
    }

    @ParameterizedTest
    @CsvSource({
        "PENDING, PREPARING",
        "PENDING, DELIVERED",
        "PENDING, PENDING",
        "CONFIRMED, READY",
        "CONFIRMED, PENDING",
        "PREPARING, OUT_FOR_DELIVERY",
        "READY, DELIVERED",
        "READY, PREPARING",
        "OUT_FOR_DELIVERY, READY",
    })
    void invalidTransitionsAreRejected(OrderStatus from, OrderStatus to) {
        order.setStatus(from);
        assertThrows(InvalidOrderStateException.class, () -> order.changeStatus(to));
        assertEquals(from, order.getStatus());
    }

    @Test
    void deliveredOrderCannotBeModified() {
        order.setStatus(OrderStatus.DELIVERED);
        InvalidOrderStateException e = assertThrows(InvalidOrderStateException.class,
                () -> order.changeStatus(OrderStatus.CANCELLED));
        assertTrue(e.getMessage().contains("delivered"));
        assertThrows(InvalidOrderStateException.class, () -> order.cancel());
    }

    @Test
    void cancelledOrderCannotBeModified() {
        order.setStatus(OrderStatus.CANCELLED);
        assertThrows(InvalidOrderStateException.class, () -> order.changeStatus(OrderStatus.CONFIRMED));
    }

    @Test
    void cancelThroughChangeStatus() {
        order.changeStatus(OrderStatus.CONFIRMED);
        order.changeStatus(OrderStatus.CANCELLED);
        assertEquals(OrderStatus.CANCELLED, order.getStatus());

        order.setStatus(OrderStatus.PREPARING);
        assertThrows(InvalidOrderStateException.class, () -> order.changeStatus(OrderStatus.CANCELLED));
    }

    @Test
    void nullStatusRejected() {
        assertThrows(IllegalArgumentException.class, () -> order.changeStatus(null));
    }

    @Test
    void billFieldsDefaultAndSetters() {
        assertEquals("NORMAL", order.getPricingType());
        assertEquals("UNPAID", order.getPaymentStatus());
        assertEquals(0, new BigDecimal("20.00").compareTo(order.getTotal()));
        order.setDeliveryFee(new BigDecimal("5"));
        order.setDiscount(new BigDecimal("2"));
        assertEquals(0, new BigDecimal("23").compareTo(order.getTotal()));
        order.setTotal(new BigDecimal("99"));
        assertEquals(new BigDecimal("99"), order.getTotal());
        order.setPricingType("PROMO");
        order.setPaymentStatus("PAID");
        assertEquals("PROMO", order.getPricingType());
        assertEquals("PAID", order.getPaymentStatus());
    }
}
