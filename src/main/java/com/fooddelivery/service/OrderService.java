package com.fooddelivery.service;

import com.fooddelivery.domain.DeliveryAddress;
import com.fooddelivery.domain.MenuItem;
import com.fooddelivery.domain.Order;
import com.fooddelivery.domain.OrderItem;
import com.fooddelivery.domain.Restaurant;
import com.fooddelivery.domain.User;
import com.fooddelivery.exception.DeliveryPolicyException;
import com.fooddelivery.exception.InvalidOrderException;
import com.fooddelivery.exception.ResourceNotFoundException;
import com.fooddelivery.repository.OrderRepository;
import com.fooddelivery.repository.RestaurantRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Places, looks up and cancels orders (US2.1 to US2.4).
 */
public class OrderService {

    private final RestaurantRepository restaurantRepository;
    private final OrderRepository orderRepository;

    /**
     * @param restaurantRepository used to look up the restaurant and its menu
     * @param orderRepository      used to store and find orders
     */
    public OrderService(RestaurantRepository restaurantRepository,
                        OrderRepository orderRepository) {
        this.restaurantRepository = restaurantRepository;
        this.orderRepository = orderRepository;
    }

    /**
     * Validates and places an order. The checks run in this order: restaurant
     * exists and is open, the order has items, every line is valid, the
     * delivery address is valid and inside the delivery zone, and the items
     * total reaches the minimum order value. Nothing is saved if any check fails.
     *
     * @param customer     who is ordering, must not be null
     * @param restaurantId the restaurant to order from
     * @param lines        the requested items and quantities
     * @param address      where to deliver
     * @return the saved order, with status Pending
     * @throws ResourceNotFoundException if the restaurant does not exist
     * @throws InvalidOrderException     if the order content is invalid
     * @throws DeliveryPolicyException   if a delivery policy is violated
     */
    public Order placeOrder(User customer, String restaurantId,
                            List<OrderLineRequest> lines, DeliveryAddress address) {
        if (customer == null) {
            throw new InvalidOrderException("A customer is required");
        }
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Restaurant not found: " + restaurantId));
        if (!restaurant.isOpen()) {
            throw new InvalidOrderException("Restaurant is closed: " + restaurant.getName());
        }

        List<OrderItem> items = buildItems(restaurant, lines);
        checkDeliveryPolicies(restaurant, address, items);

        Order order = new Order("ORD-" + (orderRepository.findAll().size() + 1),
                customer, restaurant, items, address);
        orderRepository.save(order);
        return order;
    }

    /**
     * Cancels an order that has not yet entered preparation.
     *
     * @param orderId the order id
     * @return the cancelled order
     * @throws ResourceNotFoundException                          if the order does not exist
     * @throws com.fooddelivery.exception.InvalidOrderStateException if it can no longer be cancelled
     */
    public Order cancelOrder(String orderId) {
        Order order = getOrder(orderId);
        order.cancel();
        orderRepository.save(order);
        return order;
    }

    /**
     * Finds an order.
     *
     * @param orderId the order id
     * @return the order
     * @throws ResourceNotFoundException if the order does not exist
     */
    public Order getOrder(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
    }

    /**
     * Lists a customer's orders.
     *
     * @param customer the customer
     * @return that customer's orders
     */
    public List<Order> getOrdersFor(User customer) {
        return orderRepository.findByCustomerId(customer.getId());
    }

    private List<OrderItem> buildItems(Restaurant restaurant, List<OrderLineRequest> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new InvalidOrderException("An order must contain at least one item");
        }
        List<OrderItem> items = new ArrayList<>();
        for (OrderLineRequest line : lines) {
            if (line == null || line.getQuantity() < 1) {
                throw new InvalidOrderException("Quantity must be at least 1");
            }
            MenuItem menuItem = restaurant.findMenuItem(line.getMenuItemId())
                    .orElseThrow(() -> new InvalidOrderException(
                            "Item is not on the menu: " + line.getMenuItemId()));
            if (!menuItem.isAvailable()) {
                throw new InvalidOrderException("Item is not available: " + menuItem.getName());
            }
            items.add(new OrderItem(menuItem, line.getQuantity()));
        }
        return items;
    }

    private void checkDeliveryPolicies(Restaurant restaurant, DeliveryAddress address,
                                       List<OrderItem> items) {
        if (address == null || !address.isValid()) {
            throw new DeliveryPolicyException("Invalid delivery address");
        }
        if (!restaurant.isWithinDeliveryZone(address.getLocation())) {
            throw new DeliveryPolicyException("Delivery address is outside the delivery zone of "
                    + restaurant.getName());
        }
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItem item : items) {
            total = total.add(item.getSubtotal());
        }
        if (total.compareTo(restaurant.getMinimumOrderValue()) < 0) {
            throw new DeliveryPolicyException("Order total " + total.toPlainString()
                    + " is below the minimum order value "
                    + restaurant.getMinimumOrderValue().toPlainString());
        }
    }
}
