package com.fooddelivery.repository;

import com.fooddelivery.domain.Order;

import java.util.List;
import java.util.Optional;

/**
 * Persistence abstraction for {@link Order} entities.
 */
public interface OrderRepository {

    /**
     * Saves (inserts or replaces) an order.
     *
     * @param order the order to save
     */
    void save(Order order);

    /**
     * Finds an order by id.
     *
     * @param id the order id
     * @return the order, or empty if none exists
     */
    Optional<Order> findById(String id);

    /**
     * Finds all orders placed by a customer.
     *
     * @param customerId the customer's user id
     * @return that customer's orders in insertion order
     */
    List<Order> findByCustomerId(String customerId);
}
