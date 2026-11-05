package com.fooddelivery.domain;

import static org.junit.jupiter.api.Assertions.*;

import com.fooddelivery.exception.InvalidOrderStateException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrderTest {

    private User customer;
    private Restaurant restaurant;
    private List<OrderItem> items;
    private DeliveryAddress address;

    @BeforeEach
    void setUp() {
        customer = new User("u1", "alice", "h", Role.CUSTOMER);
        restaurant = new Restaurant("r1", "Pizza", true);
        items = Arrays.asList(
                new OrderItem(new MenuItem("m1", "A", new BigDecimal("30.00"), true), 2),
                new OrderItem(new MenuItem("m2", "B", new BigDecimal("35.50"), true), 1));
        address = new DeliveryAddress("Main St", new Location(1, 1));
    }

    private Order newOrder() {
        return new Order("ORD-1", customer, restaurant, items, address);
    }

    @Test
    void newOrderIsPendingWithCorrectTotal() {
        Order order = newOrder();
        assertEquals(OrderStatus.PENDING, order.getStatus());
        assertEquals(0, new BigDecimal("95.50").compareTo(order.getItemsTotal()));
        assertEquals("ORD-1", order.getId());
        assertSame(customer, order.getCustomer());
        assertSame(restaurant, order.getRestaurant());
        assertSame(address, order.getDeliveryAddress());
        assertEquals(2, order.getItems().size());
    }

    @Test
    void itemsViewIsUnmodifiable() {
        assertThrows(UnsupportedOperationException.class, () -> newOrder().getItems().clear());
    }

    @Test
    void pendingOrderCanBeCancelled() {
        Order order = newOrder();
        order.cancel();
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
    }

    @Test
    void orderInPreparationCannotBeCancelled() {
        Order order = newOrder();
        order.setStatus(OrderStatus.PREPARING);
        assertThrows(InvalidOrderStateException.class, order::cancel);
        assertEquals(OrderStatus.PREPARING, order.getStatus());
    }

    @Test
    void alreadyCancelledOrderCannotBeCancelledAgain() {
        Order order = newOrder();
        order.cancel();
        assertThrows(InvalidOrderStateException.class, order::cancel);
    }

    @Test
    void rejectsMissingParts() {
        assertThrows(IllegalArgumentException.class,
                () -> new Order("", customer, restaurant, items, address));
        assertThrows(IllegalArgumentException.class,
                () -> new Order("o", null, restaurant, items, address));
        assertThrows(IllegalArgumentException.class,
                () -> new Order("o", customer, null, items, address));
        assertThrows(IllegalArgumentException.class,
                () -> new Order("o", customer, restaurant, items, null));
        assertThrows(IllegalArgumentException.class,
                () -> new Order("o", customer, restaurant, null, address));
        assertThrows(IllegalArgumentException.class,
                () -> new Order("o", customer, restaurant, Collections.<OrderItem>emptyList(), address));
    }

    @Test
    void nullStatusRejected() {
        assertThrows(IllegalArgumentException.class, () -> newOrder().setStatus(null));
    }

    @Test
    void equalityIsBasedOnId() {
        assertEquals(newOrder(), newOrder());
        assertEquals(newOrder().hashCode(), newOrder().hashCode());
        assertNotEquals(newOrder(), new Order("ORD-2", customer, restaurant, items, address));
        assertNotEquals(newOrder(), "x");
    }
}
