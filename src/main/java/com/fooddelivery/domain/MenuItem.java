package com.fooddelivery.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * A single item on a restaurant's menu.
 */
public class MenuItem {

    private final String id;
    private final String name;
    private final BigDecimal price;
    private boolean available;

    /**
     * Creates a menu item.
     *
     * @param id        unique identifier, must not be blank
     * @param name      display name, must not be blank
     * @param price     unit price, must not be null or negative
     * @param available whether the item can currently be ordered
     * @throws IllegalArgumentException if id/name are blank or price is null/negative
     */
    public MenuItem(String id, String name, BigDecimal price, boolean available) {
        if (id == null || id.trim().isEmpty() || name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Menu item id and name must not be blank");
        }
        if (price == null || price.signum() < 0) {
            throw new IllegalArgumentException("Menu item price must not be negative");
        }
        this.id = id;
        this.name = name;
        this.price = price;
        this.available = available;
    }

    /** @return the unique identifier */
    public String getId() {
        return id;
    }

    /** @return the display name */
    public String getName() {
        return name;
    }

    /** @return the unit price */
    public BigDecimal getPrice() {
        return price;
    }

    /** @return {@code true} if the item can currently be ordered */
    public boolean isAvailable() {
        return available;
    }

    /**
     * Marks the item as available or unavailable.
     *
     * @param available the new availability
     */
    public void setAvailable(boolean available) {
        this.available = available;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MenuItem)) {
            return false;
        }
        return id.equals(((MenuItem) o).id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
