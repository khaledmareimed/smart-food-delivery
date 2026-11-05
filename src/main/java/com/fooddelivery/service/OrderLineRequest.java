package com.fooddelivery.service;

/**
 * A request to order a quantity of one menu item. It is raw input, so it is
 * validated by {@link OrderService#placeOrder} and not by this class.
 */
public final class OrderLineRequest {

    private final String menuItemId;
    private final int quantity;

    /**
     * @param menuItemId the id of the menu item to order
     * @param quantity   how many units are requested
     */
    public OrderLineRequest(String menuItemId, int quantity) {
        this.menuItemId = menuItemId;
        this.quantity = quantity;
    }

    /** @return the menu item id */
    public String getMenuItemId() {
        return menuItemId;
    }

    /** @return the requested quantity */
    public int getQuantity() {
        return quantity;
    }
}
