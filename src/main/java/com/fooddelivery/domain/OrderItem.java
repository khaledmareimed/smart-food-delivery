package com.fooddelivery.domain;

import java.math.BigDecimal;

/**
 * One line of an order: a menu item, a quantity, and the unit price at the time
 * of ordering. The price is copied so later menu price changes do not alter
 * existing orders.
 */
public final class OrderItem {

    private final String menuItemId;
    private final String name;
    private final BigDecimal unitPrice;
    private final int quantity;

    /**
     * @param item     the ordered menu item, must not be null
     * @param quantity how many units, must be at least 1
     * @throws IllegalArgumentException if the item is null or the quantity is below 1
     */
    public OrderItem(MenuItem item, int quantity) {
        if (item == null) {
            throw new IllegalArgumentException("Menu item must not be null");
        }
        if (quantity < 1) {
            throw new IllegalArgumentException("Quantity must be at least 1");
        }
        this.menuItemId = item.getId();
        this.name = item.getName();
        this.unitPrice = item.getPrice();
        this.quantity = quantity;
    }

    /** @return the menu item id */
    public String getMenuItemId() {
        return menuItemId;
    }

    /** @return the item name at the time of ordering */
    public String getName() {
        return name;
    }

    /** @return the unit price at the time of ordering */
    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    /** @return the number of units */
    public int getQuantity() {
        return quantity;
    }

    /** @return unit price multiplied by quantity */
    public BigDecimal getSubtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
