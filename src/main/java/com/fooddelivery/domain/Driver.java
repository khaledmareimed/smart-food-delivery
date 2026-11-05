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
