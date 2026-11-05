package com.fooddelivery.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fooddelivery.domain.DeliveryAddress;
import com.fooddelivery.domain.Location;
import com.fooddelivery.domain.MenuItem;
import com.fooddelivery.domain.Order;
import com.fooddelivery.domain.OrderStatus;
import com.fooddelivery.domain.Restaurant;
import com.fooddelivery.domain.Role;
import com.fooddelivery.domain.User;
import com.fooddelivery.exception.DeliveryPolicyException;
import com.fooddelivery.exception.InvalidOrderException;
import com.fooddelivery.exception.InvalidOrderStateException;
import com.fooddelivery.exception.ResourceNotFoundException;
import com.fooddelivery.repository.OrderRepository;
import com.fooddelivery.repository.RestaurantRepository;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests US2.1 to US2.4 using mocked repositories.
 * Restaurant r1 is open at (0,0), delivers within 10 km, minimum order 50.
 */
class OrderServiceTest {

    private RestaurantRepository restaurantRepository;
    private OrderRepository orderRepository;
    private OrderService service;
    private User alice;
    private Restaurant restaurant;
    private DeliveryAddress nearAddress;

    @BeforeEach
    void setUp() {
        restaurantRepository = mock(RestaurantRepository.class);
        orderRepository = mock(OrderRepository.class);
        service = new OrderService(restaurantRepository, orderRepository);
        alice = new User("u1", "alice", "h", Role.CUSTOMER);

        restaurant = new Restaurant("r1", "Pizza Palace", true,
                new Location(0, 0), 10.0, new BigDecimal("50.00"));
        restaurant.addMenuItem(new MenuItem("m1", "Margherita", new BigDecimal("30.00"), true));
        restaurant.addMenuItem(new MenuItem("m2", "Pepperoni", new BigDecimal("35.50"), true));
        restaurant.addMenuItem(new MenuItem("m3", "Four Cheese", new BigDecimal("40.00"), false));
        when(restaurantRepository.findById("r1")).thenReturn(Optional.of(restaurant));

        nearAddress = new DeliveryAddress("Main St", new Location(3, 4));
    }

    private List<OrderLineRequest> lines(OrderLineRequest... lines) {
        return Arrays.asList(lines);
    }

    private void assertNothingSaved() {
        verify(orderRepository, never()).save(any(Order.class));
    }

    // ---------- US2.1 place an order ----------

    @Test
    void validOrderIsCreatedPendingWithCorrectTotal() {
        Order order = service.placeOrder(alice, "r1",
                lines(new OrderLineRequest("m1", 2), new OrderLineRequest("m2", 1)), nearAddress);

        assertEquals(OrderStatus.PENDING, order.getStatus());
        assertEquals(0, new BigDecimal("95.50").compareTo(order.getItemsTotal()));
        assertEquals(2, order.getItems().size());
        assertSame(alice, order.getCustomer());
        assertSame(restaurant, order.getRestaurant());
        verify(orderRepository).save(order);
    }

    @Test
    void orderIdsAreUnique() {
        when(orderRepository.findAll()).thenReturn(Collections.<Order>emptyList());
        Order first = service.placeOrder(alice, "r1", lines(new OrderLineRequest("m1", 2)), nearAddress);
        when(orderRepository.findAll()).thenReturn(Collections.singletonList(first));
        Order second = service.placeOrder(alice, "r1", lines(new OrderLineRequest("m1", 2)), nearAddress);
        assertNotEquals(first.getId(), second.getId());
    }

    @Test
    void unknownRestaurantIsRejected() {
        when(restaurantRepository.findById("zzz")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.placeOrder(alice, "zzz",
                lines(new OrderLineRequest("m1", 2)), nearAddress));
        assertNothingSaved();
    }

    @Test
    void closedRestaurantIsRejected() {
        restaurant.setOpen(false);
        assertThrows(InvalidOrderException.class, () -> service.placeOrder(alice, "r1",
                lines(new OrderLineRequest("m1", 2)), nearAddress));
        assertNothingSaved();
    }

    @Test
    void missingCustomerIsRejected() {
        assertThrows(InvalidOrderException.class, () -> service.placeOrder(null, "r1",
                lines(new OrderLineRequest("m1", 2)), nearAddress));
        assertNothingSaved();
    }

    // ---------- US2.2 invalid orders ----------

    @Test
    void emptyOrderIsRejected() {
        assertThrows(InvalidOrderException.class, () -> service.placeOrder(alice, "r1",
                Collections.<OrderLineRequest>emptyList(), nearAddress));
        assertThrows(InvalidOrderException.class,
                () -> service.placeOrder(alice, "r1", null, nearAddress));
        assertNothingSaved();
    }

    @Test
    void orderWithUnavailableItemIsRejected() {
        InvalidOrderException ex = assertThrows(InvalidOrderException.class,
                () -> service.placeOrder(alice, "r1",
                        lines(new OrderLineRequest("m1", 2), new OrderLineRequest("m3", 1)),
                        nearAddress));
        assertTrue(ex.getMessage().contains("Four Cheese"));
        assertNothingSaved();
    }

    @Test
    void itemNotOnMenuIsRejected() {
        assertThrows(InvalidOrderException.class, () -> service.placeOrder(alice, "r1",
                lines(new OrderLineRequest("nope", 1)), nearAddress));
        assertNothingSaved();
    }

    @Test
    void zeroNegativeAndNullLinesAreRejected() {
        assertThrows(InvalidOrderException.class, () -> service.placeOrder(alice, "r1",
                lines(new OrderLineRequest("m1", 0)), nearAddress));
        assertThrows(InvalidOrderException.class, () -> service.placeOrder(alice, "r1",
                lines(new OrderLineRequest("m1", -3)), nearAddress));
        assertThrows(InvalidOrderException.class, () -> service.placeOrder(alice, "r1",
                Collections.<OrderLineRequest>singletonList(null), nearAddress));
        assertNothingSaved();
    }

    // ---------- US2.3 delivery policies ----------

    @Test
    void destinationOutsideDeliveryZoneIsRejected() {
        DeliveryAddress far = new DeliveryAddress("Far Away", new Location(8, 8)); // ~11.3 km
        DeliveryPolicyException ex = assertThrows(DeliveryPolicyException.class,
                () -> service.placeOrder(alice, "r1", lines(new OrderLineRequest("m1", 2)), far));
        assertTrue(ex.getMessage().contains("outside the delivery zone"));
        assertNothingSaved();
    }

    @Test
    void destinationExactlyOnTheRadiusIsAccepted() {
        DeliveryAddress edge = new DeliveryAddress("Edge", new Location(6, 8)); // exactly 10 km
        Order order = service.placeOrder(alice, "r1", lines(new OrderLineRequest("m1", 2)), edge);
        assertEquals(OrderStatus.PENDING, order.getStatus());
    }

    @Test
    void orderBelowMinimumValueIsRejected() {
        // 1 x 30.00 = 30.00 < 50.00
        DeliveryPolicyException ex = assertThrows(DeliveryPolicyException.class,
                () -> service.placeOrder(alice, "r1", lines(new OrderLineRequest("m1", 1)), nearAddress));
        assertTrue(ex.getMessage().contains("minimum"));
        assertNothingSaved();
    }

    @Test
    void orderExactlyAtMinimumValueIsAccepted() {
        restaurant = new Restaurant("r1", "Pizza Palace", true,
                new Location(0, 0), 10.0, new BigDecimal("30.00"));
        restaurant.addMenuItem(new MenuItem("m1", "Margherita", new BigDecimal("30.00"), true));
        when(restaurantRepository.findById("r1")).thenReturn(Optional.of(restaurant));

        Order order = service.placeOrder(alice, "r1", lines(new OrderLineRequest("m1", 1)), nearAddress);
        assertEquals(OrderStatus.PENDING, order.getStatus());
    }

    @Test
    void invalidAddressesAreRejected() {
        List<OrderLineRequest> ok = lines(new OrderLineRequest("m1", 2));
        assertThrows(DeliveryPolicyException.class,
                () -> service.placeOrder(alice, "r1", ok, null));
        assertThrows(DeliveryPolicyException.class,
                () -> service.placeOrder(alice, "r1", ok, new DeliveryAddress("", new Location(1, 1))));
        assertThrows(DeliveryPolicyException.class,
                () -> service.placeOrder(alice, "r1", ok, new DeliveryAddress("Main St", null)));
        assertNothingSaved();
    }

    // ---------- US2.4 cancel ----------

    private Order existingOrder(OrderStatus status) {
        Order order = new Order("ORD-9", alice, restaurant,
                Collections.singletonList(new com.fooddelivery.domain.OrderItem(
                        restaurant.getMenu().get(0), 2)), nearAddress);
        order.setStatus(status);
        when(orderRepository.findById("ORD-9")).thenReturn(Optional.of(order));
        return order;
    }

    @Test
    void pendingOrderCanBeCancelled() {
        Order order = existingOrder(OrderStatus.PENDING);
        Order result = service.cancelOrder("ORD-9");
        assertEquals(OrderStatus.CANCELLED, result.getStatus());
        verify(orderRepository).save(order);
    }

    @Test
    void orderInPreparationCannotBeCancelled() {
        Order order = existingOrder(OrderStatus.PREPARING);
        assertThrows(InvalidOrderStateException.class, () -> service.cancelOrder("ORD-9"));
        assertEquals(OrderStatus.PREPARING, order.getStatus());
        assertNothingSaved();
    }

    @Test
    void cancellingUnknownOrderThrows() {
        when(orderRepository.findById("nope")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.cancelOrder("nope"));
    }

    @Test
    void ordersAreListedPerCustomer() {
        Order order = existingOrder(OrderStatus.PENDING);
        when(orderRepository.findByCustomerId("u1")).thenReturn(Collections.singletonList(order));
        assertEquals(1, service.getOrdersFor(alice).size());
        assertSame(order, service.getOrder("ORD-9"));
    }
}
