package com.fooddelivery.domain;

import java.util.Objects;

/**
 * A delivery driver.
 */
public class Driver {

    private String id;
    private String name;
    private Location location;
    private double speedKmh;
    private DriverStatus status;

    public Driver(String id, String name, Location location, double speedKmh) {
        if (id == null || id.trim().isEmpty() || name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Driver id and name must not be blank");
        }
        if (location == null) {
            throw new IllegalArgumentException("Driver location must not be null");
        }
        if (speedKmh <= 0) {
            throw new IllegalArgumentException("Speed must be positive");
        }
        this.id = id;
        this.name = name;
        this.location = location;
        this.speedKmh = speedKmh;
        this.status = DriverStatus.AVAILABLE;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public double getSpeedKmh() {
        return speedKmh;
    }

    public DriverStatus getStatus() {
        return status;
    }

    public void setStatus(DriverStatus status) {
        this.status = status;
    }

    /** @return the vehicle type shown in the GUI and stored in the database */
    public String getType() {
        return "STANDARD";
    }

    /** @return how far this driver can go in one trip, 0 = no limit */
    public double getMaxDistanceKm() {
        return 0;
    }

    /**
     * Checks the vehicle rules for this order. A plain driver can deliver anything.
     */
    public boolean canDeliver(Order order) {
        return true;
    }

    /** Distance from the restaurant to the customer. */
    protected double deliveryDistance(Order order) {
        return order.getRestaurant().getLocation().distanceTo(order.getDeliveryAddress().getLocation());
    }

    public boolean isAvailable() {
        return status == DriverStatus.AVAILABLE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Driver)) {
            return false;
        }
        return id.equals(((Driver) o).id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return name + " (" + id + ")";
    }
}
