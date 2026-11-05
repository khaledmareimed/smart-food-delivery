package com.fooddelivery.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.fooddelivery.domain.DeliveryAddress;
import com.fooddelivery.domain.Location;
import com.fooddelivery.domain.MenuItem;
import com.fooddelivery.domain.Order;
import com.fooddelivery.domain.OrderItem;
import com.fooddelivery.domain.OrderStatus;
import com.fooddelivery.domain.Restaurant;
import com.fooddelivery.domain.Role;
import com.fooddelivery.domain.User;
import com.fooddelivery.repository.OrderRepository;
import com.fooddelivery.repository.RestaurantRepository;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** US3.3 and US3.4: notifications on status changes, with a mocked notification service. */
class OrderNotificationTest {

    OrderRepository orders;
    NotificationService notifications;
    OrderService service;
    Order order;

    @BeforeEach
    void setUp() {
        orders = mock(OrderRepository.class);
        notifications = mock(NotificationService.class);
        service = new OrderService(mock(RestaurantRepository.class), orders, notifications);
        Restaurant r = new Restaurant("r1", "Pizza", true);
        MenuItem m = new MenuItem("m1", "A", new BigDecimal("10"), true);
        order = new Order("ORD-1", new User("u1", "alice", "h", Role.CUSTOMER), r,
                Collections.singletonList(new OrderItem(m, 1)),
                new DeliveryAddress("Main", new Location(1, 1)));
        when(orders.findById("ORD-1")).thenReturn(Optional.of(order));
    }

    @Test
    void statusChangeSendsNotification() {
        service.updateStatus("ORD-1", OrderStatus.CONFIRMED);
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
        verify(orders).save(order);
        verify(notifications).send("u1", "Your order ORD-1 is now CONFIRMED");
    }

    @Test
    void outForDeliveryNotification() {
        order.setStatus(OrderStatus.READY);
        order.setDriver(new com.fooddelivery.domain.Driver("d1", "Omar", new Location(0, 0), 20));
        service.updateStatus("ORD-1", OrderStatus.OUT_FOR_DELIVERY);
        verify(notifications).send(eq("u1"), contains("out for delivery"));
    }

    @Test
    void deliveredNotification() {
        order.setStatus(OrderStatus.OUT_FOR_DELIVERY);
        order.setDriver(new com.fooddelivery.domain.Driver("d1", "Omar", new Location(0, 0), 20));
        service.updateStatus("ORD-1", OrderStatus.DELIVERED);
        verify(notifications).send(eq("u1"), contains("has been delivered"));
    }

    @Test
    void cancelSendsNotification() {
        service.cancelOrder("ORD-1");
        verify(notifications).send(eq("u1"), contains("cancelled"));
    }

    @Test
    void worksWithoutNotificationService() {
        OrderService plain = new OrderService(mock(RestaurantRepository.class), orders);
        plain.updateStatus("ORD-1", OrderStatus.CONFIRMED);
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
        verifyNoInteractions(notifications);
    }

    @Test
    void getAllOrdersComesFromRepository() {
        when(orders.findAll()).thenReturn(Collections.singletonList(order));
        assertEquals(1, service.getAllOrders().size());
    }
}
