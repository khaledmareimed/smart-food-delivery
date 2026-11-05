package com.fooddelivery.service;

import com.fooddelivery.domain.DeliveryAddress;
import com.fooddelivery.domain.DriverStatus;
import com.fooddelivery.domain.MenuItem;
import com.fooddelivery.domain.Order;
import com.fooddelivery.domain.OrderItem;
import com.fooddelivery.domain.OrderStatus;
import com.fooddelivery.domain.Restaurant;
import com.fooddelivery.domain.User;
import com.fooddelivery.exception.DeliveryPolicyException;
import com.fooddelivery.exception.InvalidOrderException;
import com.fooddelivery.exception.InvalidOrderStateException;
import com.fooddelivery.exception.ResourceNotFoundException;
import com.fooddelivery.repository.DriverRepository;
import com.fooddelivery.repository.OrderRepository;
import com.fooddelivery.repository.RestaurantRepository;
import com.fooddelivery.util.TimeProvider;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Places, looks up and cancels orders (US2.1 to US2.4).
 */
public class OrderService {

    private final RestaurantRepository restaurantRepository;
    private final OrderRepository orderRepository;
    private NotificationService notificationService;
    private DriverRepository driverRepository;
    private PricingService pricingService;
    private TimeProvider timeProvider;

    /**
     * @param restaurantRepository used to look up the restaurant and its menu
     * @param orderRepository      used to store and find orders
     */
    public OrderService(RestaurantRepository restaurantRepository,
                        OrderRepository orderRepository) {
        this.restaurantRepository = restaurantRepository;
        this.orderRepository = orderRepository;
    }

    public OrderService(RestaurantRepository restaurantRepository,
                        OrderRepository orderRepository, NotificationService notificationService) {
        this(restaurantRepository, orderRepository);
        this.notificationService = notificationService;
    }

    public OrderService(RestaurantRepository restaurantRepository, OrderRepository orderRepository,
                        NotificationService notificationService, DriverRepository driverRepository,
                        PricingService pricingService) {
        this(restaurantRepository, orderRepository, notificationService);
        this.driverRepository = driverRepository;
        this.pricingService = pricingService;
    }

    public OrderService(RestaurantRepository restaurantRepository, OrderRepository orderRepository,
                        NotificationService notificationService, DriverRepository driverRepository,
                        PricingService pricingService, TimeProvider timeProvider) {
        this(restaurantRepository, orderRepository, notificationService, driverRepository,
                pricingService);
        this.timeProvider = timeProvider;
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
        return placeOrder(customer, restaurantId, lines, address, "NORMAL");
    }

    /**
     * Same as the other placeOrder but also calculates the bill with the given
     * pricing type (US4.2, US4.3).
     */
    public Order placeOrder(User customer, String restaurantId,
                            List<OrderLineRequest> lines, DeliveryAddress address, String pricingType) {
        return placeOrder(customer, restaurantId, lines, address, pricingType, "STANDARD", null);
    }

    /**
     * Places an order of a given type (US5.1, US5.2).
     * EXPRESS adds 10 to the delivery fee, SCHEDULED needs a delivery time in the future,
     * STANDARD follows the normal rules.
     */
    public Order placeOrder(User customer, String restaurantId, List<OrderLineRequest> lines,
                            DeliveryAddress address, String pricingType, String orderType,
                            LocalDateTime scheduledTime) {
        if (orderType == null) {
            orderType = "STANDARD";
        }
        if (orderType.equals("SCHEDULED")) {
            if (scheduledTime == null) {
                throw new InvalidOrderException("Scheduled orders need a delivery time");
            }
            LocalDateTime now = timeProvider != null ? timeProvider.now() : LocalDateTime.now();
            if (!scheduledTime.isAfter(now)) {
                throw new InvalidOrderException("Scheduled delivery time must be in the future");
            }
        } else if (!orderType.equals("STANDARD") && !orderType.equals("EXPRESS")) {
            throw new InvalidOrderException("Unknown order type: " + orderType);
        }
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
        if (pricingService != null) {
            pricingService.applyPricing(order, pricingType);
        }
        order.setOrderType(orderType);
        if (orderType.equals("EXPRESS")) {
            order.setDeliveryFee(order.getDeliveryFee().add(new BigDecimal("10.00")));
            order.setTotal(order.getItemsTotal().add(order.getDeliveryFee())
                    .subtract(order.getDiscount()));
        } else if (orderType.equals("SCHEDULED")) {
            order.setScheduledTime(scheduledTime);
        }
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
        if (order.getDriver() != null && driverRepository != null) {
            order.getDriver().setStatus(DriverStatus.AVAILABLE);
            driverRepository.save(order.getDriver());
        }
        orderRepository.save(order);
        if (notificationService != null) {
            notificationService.send(order.getCustomer().getId(),
                    "Your order " + order.getId() + " was cancelled");
        }
        return order;
    }

    /**
     * Changes the status of an order and notifies the customer (US3.3, US3.4).
     */
    public Order updateStatus(String orderId, OrderStatus newStatus) {
        Order order = getOrder(orderId);
        if (newStatus == OrderStatus.OUT_FOR_DELIVERY && order.getDriver() == null) {
            throw new InvalidOrderStateException("Assign a driver before sending the order out");
        }
        order.changeStatus(newStatus);
        if (newStatus == OrderStatus.DELIVERED && driverRepository != null) {
            // driver is free again
            order.getDriver().setStatus(DriverStatus.AVAILABLE);
            order.getDriver().setLocation(order.getDeliveryAddress().getLocation());
            driverRepository.save(order.getDriver());
        }
        orderRepository.save(order);
        if (notificationService != null) {
            String msg;
            if (newStatus == OrderStatus.OUT_FOR_DELIVERY) {
                msg = "Your order " + order.getId() + " is out for delivery";
            } else if (newStatus == OrderStatus.DELIVERED) {
                msg = "Your order " + order.getId() + " has been delivered. Enjoy your meal!";
            } else {
                msg = "Your order " + order.getId() + " is now " + newStatus;
            }
            notificationService.send(order.getCustomer().getId(), msg);
        }
        return order;
    }

    /**
     * Status change made by the driver from the driver screen. Drivers can
     * only pick up (Out for Delivery) and deliver their own orders.
     */
    public Order updateStatusByDriver(String driverId, String orderId, OrderStatus newStatus) {
        Order order = getOrder(orderId);
        if (order.getDriver() == null || !order.getDriver().getId().equals(driverId)) {
            throw new InvalidOrderStateException("Order " + orderId + " is not assigned to you");
        }
        if (newStatus != OrderStatus.OUT_FOR_DELIVERY && newStatus != OrderStatus.DELIVERED) {
            throw new InvalidOrderStateException("Drivers can only pick up or deliver orders");
        }
        return updateStatus(orderId, newStatus);
    }

    /** @return the status after the given one, or null if there is none */
    public OrderStatus nextStatus(OrderStatus status) {
        if (status == OrderStatus.PENDING) {
            return OrderStatus.CONFIRMED;
        } else if (status == OrderStatus.CONFIRMED) {
            return OrderStatus.PREPARING;
        } else if (status == OrderStatus.PREPARING) {
            return OrderStatus.READY;
        } else if (status == OrderStatus.READY) {
            return OrderStatus.OUT_FOR_DELIVERY;
        } else if (status == OrderStatus.OUT_FOR_DELIVERY) {
            return OrderStatus.DELIVERED;
        }
        return null;
    }

    /** Moves the order one step forward in its lifecycle. */
    public Order advanceStatus(String orderId) {
        Order order = getOrder(orderId);
        OrderStatus next = nextStatus(order.getStatus());
        if (next == null) {
            throw new InvalidOrderStateException("Order " + orderId + " is already " + order.getStatus());
        }
        return updateStatus(orderId, next);
    }

    /** @return every order, used by the manager screen */
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
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

    /**
     * Small text report for the manager: orders per status and money collected.
     */
    public String getSummary() {
        List<Order> all = orderRepository.findAll();
        int pending = 0;
        int active = 0;
        int delivered = 0;
        int cancelled = 0;
        BigDecimal revenue = BigDecimal.ZERO;
        BigDecimal unpaid = BigDecimal.ZERO;
        for (Order o : all) {
            if (o.getStatus() == OrderStatus.PENDING) {
                pending++;
            } else if (o.getStatus() == OrderStatus.DELIVERED) {
                delivered++;
            } else if (o.getStatus() == OrderStatus.CANCELLED) {
                cancelled++;
            } else {
                active++;
            }
            if (o.getPaymentStatus().equals("PAID")) {
                revenue = revenue.add(o.getTotal());
            } else if (o.getStatus() != OrderStatus.CANCELLED) {
                unpaid = unpaid.add(o.getTotal());
            }
        }
        return "Orders: " + all.size() + " | Pending: " + pending + " | In progress: " + active
                + " | Delivered: " + delivered + " | Cancelled: " + cancelled
                + " | Collected: " + revenue + " | Not paid yet: " + unpaid;
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
