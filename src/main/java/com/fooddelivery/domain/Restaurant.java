package com.fooddelivery.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A restaurant with an opening status and a menu.
 */
public class Restaurant {

    private final String id;
    private final String name;
    private boolean open;
    private final List<MenuItem> menu = new ArrayList<>();

    /**
     * Creates a restaurant with an empty menu.
     *
     * @param id   unique identifier, must not be blank
     * @param name display name, must not be blank
     * @param open whether the restaurant is currently open
     * @throws IllegalArgumentException if id or name is null or blank
     */
    public Restaurant(String id, String name, boolean open) {
        if (id == null || id.trim().isEmpty() || name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Restaurant id and name must not be blank");
        }
        this.id = id;
        this.name = name;
        this.open = open;
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
