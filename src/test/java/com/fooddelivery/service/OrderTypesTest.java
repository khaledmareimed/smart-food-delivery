package com.fooddelivery.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fooddelivery.domain.DeliveryAddress;
import com.fooddelivery.domain.Location;
import com.fooddelivery.domain.MenuItem;
import com.fooddelivery.domain.Order;
import com.fooddelivery.domain.Restaurant;
import com.fooddelivery.domain.Role;
import com.fooddelivery.domain.User;
import com.fooddelivery.exception.DeliveryPolicyException;
import com.fooddelivery.exception.InvalidOrderException;
import com.fooddelivery.repository.DriverRepository;
import com.fooddelivery.repository.OrderRepository;
import com.fooddelivery.repository.RestaurantRepository;
import com.fooddelivery.util.TimeProvider;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** US5.1 and US5.2 order types with a mocked clock. */
class OrderTypesTest {

    static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 20, 10, 0);

    OrderRepository orders;
    TimeProvider clock;
    OrderService service;
    User alice = new User("u1", "alice", "h", Role.CUSTOMER);
    DeliveryAddress address = new DeliveryAddress("Main", new Location(3, 4)); // 5 km
    List<OrderLineRequest> lines = Collections.singletonList(new OrderLineRequest("m1", 1));

    @BeforeEach
    void setUp() {
        RestaurantRepository restaurants = mock(RestaurantRepository.class);
        orders = mock(OrderRepository.class);
        clock = mock(TimeProvider.class);
        when(clock.now()).thenReturn(NOW);
        service = new OrderService(restaurants, orders, mock(NotificationService.class),
                mock(DriverRepository.class), new PricingService(clock), clock);
        Restaurant r = new Restaurant("r1", "Pizza", true, new Location(0, 0), 10, new BigDecimal("20"));
        r.addMenuItem(new MenuItem("m1", "Margherita", new BigDecimal("30.00"), true));
        when(restaurants.findById("r1")).thenReturn(Optional.of(r));
    }

    @Test
    void standardOrderFollowsNormalRules() {
        Order o = service.placeOrder(alice, "r1", lines, address, "NORMAL", "STANDARD", null);
        assertEquals("STANDARD", o.getOrderType());
        assertNull(o.getScheduledTime());
        assertEquals(0, new BigDecimal("42.50").compareTo(o.getTotal()));
        verify(orders).save(o);
    }

    @Test
    void nullTypeIsStandard() {
        Order o = service.placeOrder(alice, "r1", lines, address, "NORMAL", null, null);
        assertEquals("STANDARD", o.getOrderType());
    }

    @Test
    void standardOrderStillChecksDeliveryPolicies() {
        DeliveryAddress far = new DeliveryAddress("Far", new Location(50, 50));
        assertThrows(DeliveryPolicyException.class,
                () -> service.placeOrder(alice, "r1", lines, far, "NORMAL", "STANDARD", null));
    }

    @Test
    void expressOrderAddsExtraFee() {
        Order o = service.placeOrder(alice, "r1", lines, address, "NORMAL", "EXPRESS", null);
        assertEquals("EXPRESS", o.getOrderType());
        assertEquals(0, new BigDecimal("22.50").compareTo(o.getDeliveryFee()));
        assertEquals(0, new BigDecimal("52.50").compareTo(o.getTotal()));
    }

    @Test
    void expressOrderWithPromoKeepsDiscount() {
        Order o = service.placeOrder(alice, "r1", lines, address, "PROMO", "EXPRESS", null);
        // items 30, fee 12.5 + 10, discount 4.5
        assertEquals(0, new BigDecimal("48.00").compareTo(o.getTotal()));
    }

    @Test
    void scheduledOrderNeedsFutureTime() {
        LocalDateTime later = NOW.plusHours(3);
        Order o = service.placeOrder(alice, "r1", lines, address, "NORMAL", "SCHEDULED", later);
        assertEquals("SCHEDULED", o.getOrderType());
        assertEquals(later, o.getScheduledTime());

        assertThrows(InvalidOrderException.class,
                () -> service.placeOrder(alice, "r1", lines, address, "NORMAL", "SCHEDULED", null));
        assertThrows(InvalidOrderException.class,
                () -> service.placeOrder(alice, "r1", lines, address, "NORMAL", "SCHEDULED", NOW));
        assertThrows(InvalidOrderException.class, () -> service.placeOrder(alice, "r1", lines,
                address, "NORMAL", "SCHEDULED", NOW.minusMinutes(1)));
        verify(orders, times(1)).save(any(Order.class));
    }

    @Test
    void scheduledWithoutClockUsesSystemTime() {
        OrderService noClock = new OrderService(mock(RestaurantRepository.class), orders);
        assertThrows(InvalidOrderException.class, () -> noClock.placeOrder(alice, "r1", lines,
                address, "NORMAL", "SCHEDULED", LocalDateTime.now().minusDays(1)));
    }

    @Test
    void expressWithoutPricingServiceStillAddsFee() {
        RestaurantRepository restaurants = mock(RestaurantRepository.class);
        Restaurant r = new Restaurant("r1", "Pizza", true);
        r.addMenuItem(new MenuItem("m1", "Margherita", new BigDecimal("30.00"), true));
        when(restaurants.findById("r1")).thenReturn(Optional.of(r));
        OrderService basic = new OrderService(restaurants, orders);
        Order o = basic.placeOrder(alice, "r1", lines, new DeliveryAddress("A", new Location(1, 1)),
                "NORMAL", "EXPRESS", null);
        assertEquals(0, new BigDecimal("40.00").compareTo(o.getTotal()));
    }

    @Test
    void unknownTypeRejected() {
        assertThrows(InvalidOrderException.class,
                () -> service.placeOrder(alice, "r1", lines, address, "NORMAL", "DRONE", null));
    }
}
