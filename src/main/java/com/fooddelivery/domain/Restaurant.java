package com.fooddelivery.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A restaurant with an opening status, a menu, a location, a delivery zone and
 * a minimum order value.
 */
public class Restaurant {

    /** Delivery radius (km) used by the short constructor. */
    public static final double DEFAULT_DELIVERY_RADIUS_KM = 10.0;

    private final String id;
    private final String name;
    private boolean open;
    private final Location location;
    private final double deliveryRadiusKm;
    private final BigDecimal minimumOrderValue;
    private final List<MenuItem> menu = new ArrayList<>();

    /**
     * Creates a restaurant at (0, 0) with a default delivery radius and no
     * minimum order value.
     *
     * @param id   unique identifier, must not be blank
     * @param name display name, must not be blank
     * @param open whether the restaurant is currently open
     * @throws IllegalArgumentException if id or name is null or blank
     */
    public Restaurant(String id, String name, boolean open) {
        this(id, name, open, new Location(0, 0), DEFAULT_DELIVERY_RADIUS_KM, BigDecimal.ZERO);
    }

    /**
     * Creates a restaurant with an empty menu.
     *
     * @param id                unique identifier, must not be blank
     * @param name              display name, must not be blank
     * @param open              whether the restaurant is currently open
     * @param location          where the restaurant is, must not be null
     * @param deliveryRadiusKm  maximum delivery distance in km, must not be negative
     * @param minimumOrderValue minimum value of the items in an order, must not be null or negative
     * @throws IllegalArgumentException if any argument is invalid
     */
    public Restaurant(String id, String name, boolean open, Location location,
                      double deliveryRadiusKm, BigDecimal minimumOrderValue) {
        if (id == null || id.trim().isEmpty() || name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Restaurant id and name must not be blank");
        }
        if (location == null) {
            throw new IllegalArgumentException("Restaurant location must not be null");
        }
        if (deliveryRadiusKm < 0 || Double.isNaN(deliveryRadiusKm)) {
            throw new IllegalArgumentException("Delivery radius must not be negative");
        }
        if (minimumOrderValue == null || minimumOrderValue.signum() < 0) {
            throw new IllegalArgumentException("Minimum order value must not be negative");
        }
        this.id = id;
        this.name = name;
        this.open = open;
        this.location = location;
        this.deliveryRadiusKm = deliveryRadiusKm;
        this.minimumOrderValue = minimumOrderValue;
    }

    /** @return the unique identifier */
    public String getId() {
        return id;
    }

    /** @return the display name */
    public String getName() {
        return name;
    }

    /** @return {@code true} if the restaurant is currently open */
    public boolean isOpen() {
        return open;
    }

    /**
     * Opens or closes the restaurant.
     *
     * @param open the new opening status
     */
    public void setOpen(boolean open) {
        this.open = open;
    }

    /** @return where the restaurant is */
    public Location getLocation() {
        return location;
    }

    /** @return the maximum delivery distance in km */
    public double getDeliveryRadiusKm() {
        return deliveryRadiusKm;
    }

    /** @return the minimum value of the items in an order */
    public BigDecimal getMinimumOrderValue() {
        return minimumOrderValue;
    }

    /**
     * Checks whether a destination is inside this restaurant's delivery zone.
     *
     * @param destination the delivery location, must not be null
     * @return {@code true} if the distance is at most the delivery radius
     */
    public boolean isWithinDeliveryZone(Location destination) {
        return location.distanceTo(destination) <= deliveryRadiusKm;
    }

    /**
     * Adds an item to the menu.
     *
     * @param item the item to add, must not be null
     * @throws IllegalArgumentException if the item is null
     */
    public void addMenuItem(MenuItem item) {
        if (item == null) {
            throw new IllegalArgumentException("Menu item must not be null");
        }
        menu.add(item);
    }

    /** @return an unmodifiable view of the full menu, including unavailable items */
    public List<MenuItem> getMenu() {
        return Collections.unmodifiableList(menu);
    }

    /** @return a new list containing only the available menu items */
    public List<MenuItem> getAvailableMenuItems() {
        List<MenuItem> result = new ArrayList<>();
        for (MenuItem item : menu) {
            if (item.isAvailable()) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * Looks up a menu item by id, whether or not it is available.
     *
     * @param menuItemId the item id
     * @return the item, or empty if it is not on this menu
     */
    public Optional<MenuItem> findMenuItem(String menuItemId) {
        for (MenuItem item : menu) {
            if (item.getId().equals(menuItemId)) {
                return Optional.of(item);
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Restaurant)) {
            return false;
        }
        return id.equals(((Restaurant) o).id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
