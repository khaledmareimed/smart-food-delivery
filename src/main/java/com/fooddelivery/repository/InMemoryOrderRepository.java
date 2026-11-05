package com.fooddelivery.repository;

import com.fooddelivery.domain.Order;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory {@link OrderRepository} that preserves insertion order.
 */
public class InMemoryOrderRepository implements OrderRepository {

    private final Map<String, Order> orders = new LinkedHashMap<>();

    @Override
    public void save(Order order) {
        orders.put(order.getId(), order);
    }

    @Override
    public Optional<Order> findById(String id) {
        return Optional.ofNullable(orders.get(id));
    }

    @Override
    public List<Order> findByCustomerId(String customerId) {
        List<Order> result = new ArrayList<>();
        for (Order order : orders.values()) {
            if (order.getCustomer().getId().equals(customerId)) {
                result.add(order);
            }
        }
        return result;
    }

    @Override
    public List<Order> findAll() {
        return new ArrayList<>(orders.values());
    }
}
