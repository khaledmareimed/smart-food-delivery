package com.fooddelivery.service;

import com.fooddelivery.domain.Driver;
import com.fooddelivery.domain.DriverStatus;
import com.fooddelivery.domain.Order;
import com.fooddelivery.domain.OrderStatus;
import com.fooddelivery.exception.InvalidOrderStateException;
import com.fooddelivery.exception.NoDriverAvailableException;
import com.fooddelivery.exception.ResourceNotFoundException;
import com.fooddelivery.repository.DriverRepository;
import com.fooddelivery.repository.OrderRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Assigns drivers to orders (US3.1, US3.2) and notifies the customer (US3.4).
 */
public class DriverService {

    private DriverRepository driverRepository;
    private OrderRepository orderRepository;
    private NotificationService notificationService;

    public DriverService(DriverRepository driverRepository, OrderRepository orderRepository,
                         NotificationService notificationService) {
        this.driverRepository = driverRepository;
        this.orderRepository = orderRepository;
        this.notificationService = notificationService;
    }

    public List<Driver> getAllDrivers() {
        return driverRepository.findAll();
    }

    public Driver getDriver(String driverId) {
        return driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found: " + driverId));
    }

    /** @return every order that was given to this driver, oldest first */
    public List<Order> getOrdersForDriver(String driverId) {
        return orderRepository.findByDriverId(driverId);
    }

    /** @return the order the driver is delivering now, or null */
    public Order getCurrentOrder(String driverId) {
        for (Order o : orderRepository.findByDriverId(driverId)) {
            if (o.getStatus() != OrderStatus.DELIVERED && o.getStatus() != OrderStatus.CANCELLED) {
                return o;
            }
        }
        return null;
    }

    /**
     * Driver goes online (available) or offline (busy). A driver with an
     * active delivery cannot go online.
     */
    public Driver setAvailable(String driverId, boolean available) {
        Driver driver = getDriver(driverId);
        if (available && getCurrentOrder(driverId) != null) {
            throw new InvalidOrderStateException("Finish your current delivery first");
        }
        if (available) {
            driver.setStatus(DriverStatus.AVAILABLE);
        } else {
            driver.setStatus(DriverStatus.BUSY);
        }
        driverRepository.save(driver);
        return driver;
    }

    public List<Driver> getAvailableDrivers() {
        List<Driver> result = new ArrayList<>();
        for (Driver d : driverRepository.findAll()) {
            if (d.getStatus() == DriverStatus.AVAILABLE) {
                result.add(d);
            }
        }
        return result;
    }

    /**
     * Assigns a specific driver to an order.
     */
    public Order assignDriver(String orderId, String driverId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found: " + driverId));
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.DELIVERED) {
            throw new InvalidOrderStateException("Cannot assign a driver to a "
                    + order.getStatus() + " order");
        }
        if (order.getDriver() != null) {
            throw new InvalidOrderStateException("Order already has a driver");
        }
        if (driver.getStatus() != DriverStatus.AVAILABLE) {
            throw new NoDriverAvailableException("Driver " + driver.getName() + " is busy");
        }
        if (!driver.canDeliver(order)) {
            throw new NoDriverAvailableException(driver.getType() + " driver " + driver.getName()
                    + " cannot deliver this order (distance or size)");
        }
        driver.setStatus(DriverStatus.BUSY);
        order.setDriver(driver);
        driverRepository.save(driver);
        orderRepository.save(order);
        notificationService.send(order.getCustomer().getId(), "Driver " + driver.getName()
                + " has been assigned to your order " + order.getId());
        notificationService.send(driver.getId(), "New delivery " + order.getId() + ": pick up at "
                + order.getRestaurant().getName() + ", deliver to " + order.getDeliveryAddress().getStreet());
        return order;
    }

    /**
     * Picks a driver automatically. mode is "NEAREST" or "FASTEST".
     */
    public Order assignBestDriver(String orderId, String mode) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.DELIVERED) {
            throw new InvalidOrderStateException("Cannot assign a driver to a "
                    + order.getStatus() + " order");
        }
        if (order.getDriver() != null) {
            throw new InvalidOrderStateException("Order already has a driver");
        }
        Driver best = null;
        if (mode.equals("NEAREST")) {
            double bestDistance = Double.MAX_VALUE;
            for (Driver d : driverRepository.findAll()) {
                if (d.getStatus() == DriverStatus.AVAILABLE && d.canDeliver(order)) {
                    double dist = d.getLocation().distanceTo(order.getRestaurant().getLocation());
                    if (dist < bestDistance) {
                        bestDistance = dist;
                        best = d;
                    }
                }
            }
        } else if (mode.equals("FASTEST")) {
            double bestTime = Double.MAX_VALUE;
            for (Driver d : driverRepository.findAll()) {
                if (d.getStatus() == DriverStatus.AVAILABLE && d.canDeliver(order)) {
                    double dist = d.getLocation().distanceTo(order.getRestaurant().getLocation())
                            + order.getRestaurant().getLocation()
                            .distanceTo(order.getDeliveryAddress().getLocation());
                    double time = dist / d.getSpeedKmh();
                    if (time < bestTime) {
                        bestTime = time;
                        best = d;
                    }
                }
            }
        } else {
            throw new IllegalArgumentException("Unknown selection mode: " + mode);
        }
        if (best == null) {
            throw new NoDriverAvailableException("No eligible driver is available right now");
        }
        best.setStatus(DriverStatus.BUSY);
        order.setDriver(best);
        driverRepository.save(best);
        orderRepository.save(order);
        notificationService.send(order.getCustomer().getId(), "Driver " + best.getName()
                + " has been assigned to your order " + order.getId());
        notificationService.send(best.getId(), "New delivery " + order.getId() + ": pick up at "
                + order.getRestaurant().getName() + ", deliver to " + order.getDeliveryAddress().getStreet());
        return order;
    }
}
