package com.fooddelivery.domain;

import com.fooddelivery.exception.InvalidOrderStateException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A customer's food order. A new order always starts in
 * {@link OrderStatus#PENDING}.
 */
public class Order {

    private final String id;
    private final User customer;
    private final Restaurant restaurant;
    private final List<OrderItem> items;
    private final DeliveryAddress deliveryAddress;
    private OrderStatus status = OrderStatus.PENDING;
    private Driver driver;

    /**
     * Creates a pending order.
     *
     * @param id              unique identifier, must not be blank
     * @param customer        who placed the order, must not be null
     * @param restaurant      the restaurant preparing the order, must not be null
     * @param items           the order lines, must not be null or empty
     * @param deliveryAddress where to deliver, must not be null
     * @throws IllegalArgumentException if any argument is missing or empty
     */
    public Order(String id, User customer, Restaurant restaurant,
                 List<OrderItem> items, DeliveryAddress deliveryAddress) {
        if (id == null || id.trim().isEmpty() || customer == null || restaurant == null
                || deliveryAddress == null || items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Order requires an id, customer, restaurant, "
                    + "address and at least one item");
        }
        this.id = id;
        this.customer = customer;
        this.restaurant = restaurant;
        this.items = new ArrayList<>(items);
        this.deliveryAddress = deliveryAddress;
    }

    /** @return the unique identifier */
    public String getId() {
        return id;
    }

    /** @return the customer who placed the order */
    public User getCustomer() {
        return customer;
    }

    /** @return the restaurant preparing the order */
    public Restaurant getRestaurant() {
        return restaurant;
    }

    /** @return an unmodifiable view of the order lines */
    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    /** @return where the order is delivered */
    public DeliveryAddress getDeliveryAddress() {
        return deliveryAddress;
    }

    /** @return the current status */
    public OrderStatus getStatus() {
        return status;
    }

    /**
     * Sets the status without any transition check.
     * <p>
     * Sprint 4 (US4.1) replaces this with validated state transitions; until
     * then it is used by services and tests to put an order into a given state.
     *
     * @param status the new status, must not be null
     * @throws IllegalArgumentException if the status is null
     */
    public void setStatus(OrderStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Status must not be null");
        }
        this.status = status;
    }

    /** @return the assigned driver, or {@code null} if none yet */
    public Driver getDriver() {
        return driver;
    }

    /**
     * Records the driver delivering this order.
     *
     * @param driver the assigned driver
     */
    public void setDriver(Driver driver) {
        this.driver = driver;
    }

    /**
     * @return the sum of all line subtotals. The delivery fee is not included;
     *         it is added by the pricing logic in Sprint 4.
     */
    public BigDecimal getItemsTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItem item : items) {
            total = total.add(item.getSubtotal());
        }
        return total;
    }

    /**
     * Cancels the order if it has not yet entered preparation.
     *
     * @throws InvalidOrderStateException if the order can no longer be cancelled
     */
    public void cancel() {
        if (!status.isCancellable()) {
            throw new InvalidOrderStateException(
                    "Order " + id + " cannot be cancelled when it is " + status);
        }
        status = OrderStatus.CANCELLED;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Order)) {
            return false;
        }
        return id.equals(((Order) o).id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
