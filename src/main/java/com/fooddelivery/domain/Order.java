package com.fooddelivery.domain;

import com.fooddelivery.exception.InvalidOrderStateException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A customer's food order.
 * <p>
 * A new order always starts in {@link OrderStatus#PENDING} and moves through
 * Pending, Confirmed, Preparing, Ready, Out for Delivery and Delivered. It also
 * keeps the bill (delivery fee, discount, total), the payment state, the
 * assigned driver and the order type.
 *
 * @author khaledmareimed
 * @author zainsiksik
 */
public class Order {

    /** Unique order identifier, for example "ORD-3". */
    private final String id;
    /** The customer who placed the order. */
    private final User customer;
    /** The restaurant that prepares the order. */
    private final Restaurant restaurant;
    /** The ordered lines (menu item and quantity). */
    private final List<OrderItem> items;
    /** Where the order is delivered. */
    private final DeliveryAddress deliveryAddress;
    /** Current lifecycle state; every order starts as Pending. */
    private OrderStatus status = OrderStatus.PENDING;
    /** The assigned driver, {@code null} until a driver is assigned. */
    private Driver driver;
    /** Pricing type used for the bill: NORMAL, PEAK, EXPRESS or PROMO. */
    private String pricingType = "NORMAL";
    /** Delivery fee included in the bill. */
    private BigDecimal deliveryFee = BigDecimal.ZERO;
    /** Discount taken off the bill. */
    private BigDecimal discount = BigDecimal.ZERO;
    /** Final bill; {@code null} means it is calculated from the other amounts. */
    private BigDecimal total;
    /** Payment state: "UNPAID" or "PAID". */
    private String paymentStatus = "UNPAID";
    /** Order type: STANDARD, EXPRESS or SCHEDULED. */
    private String orderType = "STANDARD";
    /** Requested delivery time for scheduled orders, otherwise {@code null}. */
    private LocalDateTime scheduledTime;

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
     * Moves the order to the next state of its lifecycle
     * (Pending, Confirmed, Preparing, Ready, Out for Delivery, Delivered).
     *
     * @param newStatus the requested status
     * @throws InvalidOrderStateException if the transition is not allowed
     */
    public void changeStatus(OrderStatus newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("Status must not be null");
        }
        if (status == OrderStatus.DELIVERED) {
            throw new InvalidOrderStateException("Order " + id + " is delivered and cannot be modified");
        }
        if (status == OrderStatus.CANCELLED) {
            throw new InvalidOrderStateException("Order " + id + " is cancelled and cannot be modified");
        }
        if (newStatus == OrderStatus.CANCELLED) {
            cancel();
            return;
        }
        boolean ok = false;
        if (status == OrderStatus.PENDING && newStatus == OrderStatus.CONFIRMED) {
            ok = true;
        } else if (status == OrderStatus.CONFIRMED && newStatus == OrderStatus.PREPARING) {
            ok = true;
        } else if (status == OrderStatus.PREPARING && newStatus == OrderStatus.READY) {
            ok = true;
        } else if (status == OrderStatus.READY && newStatus == OrderStatus.OUT_FOR_DELIVERY) {
            ok = true;
        } else if (status == OrderStatus.OUT_FOR_DELIVERY && newStatus == OrderStatus.DELIVERED) {
            ok = true;
        }
        if (!ok) {
            throw new InvalidOrderStateException("Cannot change order " + id + " from "
                    + status + " to " + newStatus);
        }
        status = newStatus;
    }

    /** @return how many items (sum of quantities) are in the order */
    public int getItemCount() {
        int count = 0;
        for (OrderItem item : items) {
            count += item.getQuantity();
        }
        return count;
    }

    /** @return STANDARD, EXPRESS or SCHEDULED */
    public String getOrderType() {
        return orderType;
    }

    /** @param orderType STANDARD, EXPRESS or SCHEDULED */
    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    /** @return when a scheduled order should be delivered, or {@code null} */
    public LocalDateTime getScheduledTime() {
        return scheduledTime;
    }

    /** @param scheduledTime when a scheduled order should be delivered */
    public void setScheduledTime(LocalDateTime scheduledTime) {
        this.scheduledTime = scheduledTime;
    }

    /** @return the pricing type used for the bill */
    public String getPricingType() {
        return pricingType;
    }

    /** @param pricingType the pricing type used for the bill */
    public void setPricingType(String pricingType) {
        this.pricingType = pricingType;
    }

    /** @return the delivery fee */
    public BigDecimal getDeliveryFee() {
        return deliveryFee;
    }

    /** @param deliveryFee the delivery fee */
    public void setDeliveryFee(BigDecimal deliveryFee) {
        this.deliveryFee = deliveryFee;
    }

    /** @return the discount taken off the bill */
    public BigDecimal getDiscount() {
        return discount;
    }

    /** @param discount the discount taken off the bill */
    public void setDiscount(BigDecimal discount) {
        this.discount = discount;
    }

    /**
     * @return the final bill (items + delivery fee - discount). If pricing was
     *         never applied this is the items total plus the delivery fee.
     */
    public BigDecimal getTotal() {
        if (total == null) {
            return getItemsTotal().add(deliveryFee).subtract(discount);
        }
        return total;
    }

    /** @param total the final bill */
    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    /** @return "UNPAID" or "PAID" */
    public String getPaymentStatus() {
        return paymentStatus;
    }

    /** @param paymentStatus "UNPAID" or "PAID" */
    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
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

    /**
     * Two orders are equal when they have the same id.
     *
     * @param o the other object
     * @return {@code true} if {@code o} is an order with the same id
     */
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

    /** @return a hash code based on the id */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
