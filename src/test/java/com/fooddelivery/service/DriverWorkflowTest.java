package com.fooddelivery.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.fooddelivery.domain.DeliveryAddress;
import com.fooddelivery.domain.Driver;
import com.fooddelivery.domain.DriverStatus;
import com.fooddelivery.domain.Location;
import com.fooddelivery.domain.MenuItem;
import com.fooddelivery.domain.Order;
import com.fooddelivery.domain.OrderItem;
import com.fooddelivery.domain.OrderStatus;
import com.fooddelivery.domain.Restaurant;
import com.fooddelivery.domain.Role;
import com.fooddelivery.domain.User;
import com.fooddelivery.exception.InvalidOrderStateException;
import com.fooddelivery.exception.ResourceNotFoundException;
import com.fooddelivery.repository.DriverRepository;
import com.fooddelivery.repository.OrderRepository;
import com.fooddelivery.repository.RestaurantRepository;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** What a driver does from the driver screen. */
class DriverWorkflowTest {

    DriverRepository drivers;
    OrderRepository orders;
    NotificationService notifications;
    DriverService driverService;
    OrderService orderService;
    Driver omar;
    Order order;

    @BeforeEach
    void setUp() {
        drivers = mock(DriverRepository.class);
        orders = mock(OrderRepository.class);
        notifications = mock(NotificationService.class);
        driverService = new DriverService(drivers, orders, notifications);
        orderService = new OrderService(mock(RestaurantRepository.class), orders, notifications,
                drivers, null);
        omar = new Driver("d1", "Omar", new Location(1, 1), 20);
        when(drivers.findById("d1")).thenReturn(Optional.of(omar));
        when(drivers.findAll()).thenReturn(Collections.singletonList(omar));
        MenuItem m = new MenuItem("m1", "A", new BigDecimal("10"), true);
        order = new Order("ORD-1", new User("u1", "alice", "h", Role.CUSTOMER),
                new Restaurant("r1", "Pizza", true), Collections.singletonList(new OrderItem(m, 1)),
                new DeliveryAddress("Main St", new Location(2, 2)));
        when(orders.findById("ORD-1")).thenReturn(Optional.of(order));
    }

    @Test
    void driverIsNotifiedWhenAssigned() {
        driverService.assignDriver("ORD-1", "d1");
        verify(notifications).send(eq("d1"), contains("New delivery ORD-1"));
        when(orders.findByDriverId("d1")).thenReturn(Collections.singletonList(order));
        assertSame(order, driverService.getCurrentOrder("d1"));
        assertEquals(1, driverService.getOrdersForDriver("d1").size());
    }

    @Test
    void autoAssignAlsoNotifiesDriver() {
        driverService.assignBestDriver("ORD-1", "NEAREST");
        verify(notifications).send(eq("d1"), contains("Pizza"));
    }

    @Test
    void finishedOrdersAreNotCurrent() {
        Order done = new Order("ORD-0", order.getCustomer(), order.getRestaurant(), order.getItems(),
                order.getDeliveryAddress());
        done.setStatus(OrderStatus.DELIVERED);
        Order cancelled = new Order("ORD-2", order.getCustomer(), order.getRestaurant(),
                order.getItems(), order.getDeliveryAddress());
        cancelled.setStatus(OrderStatus.CANCELLED);
        when(orders.findByDriverId("d1")).thenReturn(Arrays.asList(done, cancelled));
        assertNull(driverService.getCurrentOrder("d1"));
    }

    @Test
    void driverCanGoOfflineAndOnline() {
        driverService.setAvailable("d1", false);
        assertEquals(DriverStatus.BUSY, omar.getStatus());
        driverService.setAvailable("d1", true);
        assertEquals(DriverStatus.AVAILABLE, omar.getStatus());
        verify(drivers, times(2)).save(omar);
    }

    @Test
    void cannotGoOnlineDuringDelivery() {
        when(orders.findByDriverId("d1")).thenReturn(Collections.singletonList(order));
        assertThrows(InvalidOrderStateException.class, () -> driverService.setAvailable("d1", true));
        assertThrows(ResourceNotFoundException.class, () -> driverService.getDriver("nobody"));
    }

    @Test
    void driverPicksUpAndDeliversOwnOrder() {
        order.setDriver(omar);
        omar.setStatus(DriverStatus.BUSY);
        order.setStatus(OrderStatus.READY);
        orderService.updateStatusByDriver("d1", "ORD-1", OrderStatus.OUT_FOR_DELIVERY);
        orderService.updateStatusByDriver("d1", "ORD-1", OrderStatus.DELIVERED);
        assertEquals(OrderStatus.DELIVERED, order.getStatus());
        assertEquals(DriverStatus.AVAILABLE, omar.getStatus());
    }

    @Test
    void driverCannotTouchOtherOrdersOrOtherStatuses() {
        assertThrows(InvalidOrderStateException.class,
                () -> orderService.updateStatusByDriver("d1", "ORD-1", OrderStatus.DELIVERED));
        order.setDriver(new Driver("d2", "Sara", new Location(0, 0), 30));
        assertThrows(InvalidOrderStateException.class,
                () -> orderService.updateStatusByDriver("d1", "ORD-1", OrderStatus.DELIVERED));
        order.setDriver(omar);
        assertThrows(InvalidOrderStateException.class,
                () -> orderService.updateStatusByDriver("d1", "ORD-1", OrderStatus.CONFIRMED));
    }

    @Test
    void nextStatusAndAdvance() {
        assertEquals(OrderStatus.CONFIRMED, orderService.nextStatus(OrderStatus.PENDING));
        assertEquals(OrderStatus.PREPARING, orderService.nextStatus(OrderStatus.CONFIRMED));
        assertEquals(OrderStatus.READY, orderService.nextStatus(OrderStatus.PREPARING));
        assertEquals(OrderStatus.OUT_FOR_DELIVERY, orderService.nextStatus(OrderStatus.READY));
        assertEquals(OrderStatus.DELIVERED, orderService.nextStatus(OrderStatus.OUT_FOR_DELIVERY));
        assertNull(orderService.nextStatus(OrderStatus.DELIVERED));
        assertNull(orderService.nextStatus(OrderStatus.CANCELLED));

        orderService.advanceStatus("ORD-1");
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
        order.setStatus(OrderStatus.DELIVERED);
        assertThrows(InvalidOrderStateException.class, () -> orderService.advanceStatus("ORD-1"));
    }

    @Test
    void cancellingFreesTheDriver() {
        order.setDriver(omar);
        omar.setStatus(DriverStatus.BUSY);
        orderService.cancelOrder("ORD-1");
        assertEquals(DriverStatus.AVAILABLE, omar.getStatus());
        verify(drivers).save(omar);
    }
}
