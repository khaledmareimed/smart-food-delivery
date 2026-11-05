package com.fooddelivery.repository;

import static org.junit.jupiter.api.Assertions.*;

import com.fooddelivery.domain.DeliveryAddress;
import com.fooddelivery.domain.Location;
import com.fooddelivery.domain.MenuItem;
import com.fooddelivery.domain.Order;
import com.fooddelivery.domain.OrderItem;
import com.fooddelivery.domain.Restaurant;
import com.fooddelivery.domain.Role;
import com.fooddelivery.domain.User;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class InMemoryOrderRepositoryTest {

    private Order orderFor(String orderId, User customer) {
        return new Order(orderId, customer, new Restaurant("r1", "P", true),
                Collections.singletonList(
                        new OrderItem(new MenuItem("m1", "A", BigDecimal.TEN, true), 1)),
                new DeliveryAddress("Main St", new Location(1, 1)));
    }

    @Test
    void savedOrderCanBeFoundById() {
        InMemoryOrderRepository repo = new InMemoryOrderRepository();
        repo.save(orderFor("ORD-1", new User("u1", "alice", "h", Role.CUSTOMER)));
        assertTrue(repo.findById("ORD-1").isPresent());
        assertFalse(repo.findById("nope").isPresent());
    }

    @Test
    void findByCustomerReturnsOnlyThatCustomersOrders() {
        InMemoryOrderRepository repo = new InMemoryOrderRepository();
        User alice = new User("u1", "alice", "h", Role.CUSTOMER);
        User carol = new User("u3", "carol", "h", Role.CUSTOMER);
        repo.save(orderFor("ORD-1", alice));
        repo.save(orderFor("ORD-2", carol));
        repo.save(orderFor("ORD-3", alice));

        List<Order> result = repo.findByCustomerId("u1");
        assertEquals(2, result.size());
        assertEquals("ORD-1", result.get(0).getId());
        assertEquals("ORD-3", result.get(1).getId());
        assertTrue(repo.findByCustomerId("nobody").isEmpty());
    }
}
