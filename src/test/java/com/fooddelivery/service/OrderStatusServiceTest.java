package com.fooddelivery.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fooddelivery.domain.DeliveryAddress;
import com.fooddelivery.domain.Driver;
import com.fooddelivery.domain.DriverStatus;
import com.fooddelivery.domain.Location;
import com.fooddelivery.domain.MenuItem;
import com.fooddelivery.domain.Order;
import com.fooddelivery.domain.OrderStatus;
import com.fooddelivery.domain.Restaurant;
import com.fooddelivery.domain.Role;
import com.fooddelivery.domain.User;
import com.fooddelivery.exception.InvalidOrderStateException;
import com.fooddelivery.repository.DriverRepository;
import com.fooddelivery.repository.OrderRepository;
import com.fooddelivery.repository.RestaurantRepository;
import com.fooddelivery.util.TimeProvider;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** US4.1 through the service, plus pricing at order time. */
class OrderStatusServiceTest {

    RestaurantRepository restaurants;
    OrderRepository orders;
    DriverRepository drivers;
    NotificationService notifications;
    OrderService service;
    Restaurant restaurant;
    User alice;

    @BeforeEach
    void setUp() {
        restaurants = mock(RestaurantRepository.class);
        orders = mock(OrderRepository.class);
        drivers = mock(DriverRepository.class);
        notifications = mock(NotificationService.class);
        TimeProvider clock = mock(TimeProvider.class);
        when(clock.now()).thenReturn(LocalDateTime.of(2026, 10, 1, 9, 0));
        service = new OrderService(restaurants, orders, notifications, drivers, new PricingService(clock));
        restaurant = new Restaurant("r1", "Pizza", true, new Location(0, 0), 10, new BigDecimal("10"));
        restaurant.addMenuItem(new MenuItem("m1", "Margherita", new BigDecimal("30.00"), true));
        when(restaurants.findById("r1")).thenReturn(Optional.of(restaurant));
        alice = new User("u1", "alice", "h", Role.CUSTOMER);
    }

    private Order place(String pricing) {
        Order o = service.placeOrder(alice, "r1",
                Collections.singletonList(new OrderLineRequest("m1", 1)),
                new DeliveryAddress("Main", new Location(3, 4)), pricing);
        when(orders.findById(o.getId())).thenReturn(Optional.of(o));
        return o;
    }

    @Test
    void placeOrderCalculatesBill() {
        Order o = place("EXPRESS");
        assertEquals("EXPRESS", o.getPricingType());
        assertEquals(0, new BigDecimal("55.00").compareTo(o.getTotal()));
        Order normal = place("NORMAL");
        assertEquals(0, new BigDecimal("42.50").compareTo(normal.getTotal()));
    }

    @Test
    void validTransitionsThroughService() {
        Order o = place("NORMAL");
        Driver d = new Driver("d1", "Omar", new Location(1, 1), 20);
        d.setStatus(DriverStatus.BUSY);
        o.setDriver(d);

        service.updateStatus(o.getId(), OrderStatus.CONFIRMED);
        service.updateStatus(o.getId(), OrderStatus.PREPARING);
        service.updateStatus(o.getId(), OrderStatus.READY);
        service.updateStatus(o.getId(), OrderStatus.OUT_FOR_DELIVERY);
        service.updateStatus(o.getId(), OrderStatus.DELIVERED);

        assertEquals(OrderStatus.DELIVERED, o.getStatus());
        assertEquals(DriverStatus.AVAILABLE, d.getStatus());
        assertEquals(new Location(3, 4), d.getLocation());
        verify(drivers).save(d);
        verify(notifications, times(5)).send(eq("u1"), any(String.class));
    }

    @Test
    void invalidTransitionRejectedWithoutNotification() {
        Order o = place("NORMAL");
        assertThrows(InvalidOrderStateException.class,
                () -> service.updateStatus(o.getId(), OrderStatus.READY));
        assertEquals(OrderStatus.PENDING, o.getStatus());
        verify(notifications, never()).send(any(String.class), any(String.class));
    }

    @Test
    void cannotGoOutForDeliveryWithoutDriver() {
        Order o = place("NORMAL");
        o.setStatus(OrderStatus.READY);
        assertThrows(InvalidOrderStateException.class,
                () -> service.updateStatus(o.getId(), OrderStatus.OUT_FOR_DELIVERY));
    }

    @Test
    void deliveredOrderCannotBeChanged() {
        Order o = place("NORMAL");
        o.setStatus(OrderStatus.DELIVERED);
        assertThrows(InvalidOrderStateException.class,
                () -> service.updateStatus(o.getId(), OrderStatus.CONFIRMED));
        assertThrows(InvalidOrderStateException.class, () -> service.cancelOrder(o.getId()));
    }
}
