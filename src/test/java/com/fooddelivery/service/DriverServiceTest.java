package com.fooddelivery.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
import com.fooddelivery.exception.NoDriverAvailableException;
import com.fooddelivery.exception.ResourceNotFoundException;
import com.fooddelivery.repository.DriverRepository;
import com.fooddelivery.repository.OrderRepository;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** US3.1, US3.2 and the driver-assigned part of US3.4. */
class DriverServiceTest {

    DriverRepository drivers;
    OrderRepository orders;
    NotificationService notifications;
    DriverService service;
    Order order;
    Driver near;
    Driver far;
    Driver busy;

    @BeforeEach
    void setUp() {
        drivers = mock(DriverRepository.class);
        orders = mock(OrderRepository.class);
        notifications = mock(NotificationService.class);
        service = new DriverService(drivers, orders, notifications);

        Restaurant r = new Restaurant("r1", "Pizza", true, new Location(0, 0), 10, BigDecimal.ZERO);
        MenuItem m = new MenuItem("m1", "Margherita", new BigDecimal("30"), true);
        r.addMenuItem(m);
        order = new Order("ORD-1", new User("u1", "alice", "h", Role.CUSTOMER), r,
                Collections.singletonList(new OrderItem(m, 1)),
                new DeliveryAddress("Main", new Location(10, 0)));
        when(orders.findById("ORD-1")).thenReturn(Optional.of(order));

        near = new Driver("d1", "Near Slow", new Location(1, 0), 10);   // 11 km / 10 = 1.1 h
        far = new Driver("d2", "Far Fast", new Location(5, 0), 60);     // 15 km / 60 = 0.25 h
        busy = new Driver("d3", "Busy Closest", new Location(0, 0), 100);
        busy.setStatus(DriverStatus.BUSY);
        when(drivers.findAll()).thenReturn(Arrays.asList(near, far, busy));
        when(drivers.findById("d1")).thenReturn(Optional.of(near));
        when(drivers.findById("d3")).thenReturn(Optional.of(busy));
    }

    @Test
    void availableDriverIsAssignedAndBecomesBusy() {
        Order result = service.assignDriver("ORD-1", "d1");

        assertSame(near, result.getDriver());
        assertEquals(DriverStatus.BUSY, near.getStatus());
        verify(drivers).save(near);
        verify(orders).save(order);
        verify(notifications).send(eq("u1"), contains("Near Slow"));
    }

    @Test
    void busyDriverCannotBeAssigned() {
        assertThrows(NoDriverAvailableException.class, () -> service.assignDriver("ORD-1", "d3"));
        assertNull(order.getDriver());
        verify(notifications, never()).send(anyString(), anyString());
    }

    @Test
    void unknownOrderOrDriver() {
        assertThrows(ResourceNotFoundException.class, () -> service.assignDriver("nope", "d1"));
        assertThrows(ResourceNotFoundException.class, () -> service.assignDriver("ORD-1", "nope"));
        assertThrows(ResourceNotFoundException.class, () -> service.assignBestDriver("nope", "NEAREST"));
    }

    @Test
    void cannotAssignTwiceOrToFinishedOrder() {
        service.assignDriver("ORD-1", "d1");
        assertThrows(InvalidOrderStateException.class, () -> service.assignDriver("ORD-1", "d1"));
        assertThrows(InvalidOrderStateException.class,
                () -> service.assignBestDriver("ORD-1", "NEAREST"));

        order.setDriver(null);
        order.setStatus(OrderStatus.CANCELLED);
        assertThrows(InvalidOrderStateException.class, () -> service.assignDriver("ORD-1", "d1"));
        assertThrows(InvalidOrderStateException.class,
                () -> service.assignBestDriver("ORD-1", "FASTEST"));
        order.setStatus(OrderStatus.DELIVERED);
        assertThrows(InvalidOrderStateException.class, () -> service.assignDriver("ORD-1", "d1"));
        assertThrows(InvalidOrderStateException.class,
                () -> service.assignBestDriver("ORD-1", "FASTEST"));
    }

    @Test
    void nearestStrategyPicksClosestAvailableDriver() {
        Order result = service.assignBestDriver("ORD-1", "NEAREST");
        assertSame(near, result.getDriver());
        assertEquals(DriverStatus.BUSY, near.getStatus());
        assertEquals(DriverStatus.AVAILABLE, far.getStatus());
        verify(notifications).send(eq("u1"), anyString());
    }

    @Test
    void fastestStrategyPicksQuickestAvailableDriver() {
        Order result = service.assignBestDriver("ORD-1", "FASTEST");
        assertSame(far, result.getDriver());
        verify(drivers).save(far);
    }

    @Test
    void noAvailableDriverIsRejected() {
        near.setStatus(DriverStatus.BUSY);
        far.setStatus(DriverStatus.BUSY);
        assertThrows(NoDriverAvailableException.class,
                () -> service.assignBestDriver("ORD-1", "NEAREST"));
        assertThrows(NoDriverAvailableException.class,
                () -> service.assignBestDriver("ORD-1", "FASTEST"));
        verify(orders, never()).save(any(Order.class));
    }

    @Test
    void unknownModeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.assignBestDriver("ORD-1", "RANDOM"));
    }

    @Test
    void availableDriversList() {
        assertEquals(Arrays.asList(near, far), service.getAvailableDrivers());
        assertEquals(3, service.getAllDrivers().size());
    }
}
