package com.fooddelivery.domain;

/**
 * Where an order should be delivered: a street description plus a map location.
 * <p>
 * The constructor does not throw on bad input; use {@link #isValid()} so the
 * delivery-policy check (US2.3) can reject invalid addresses in one place.
 */
public final class DeliveryAddress {

    private final String street;
    private final Location location;

    /**
     * @param street   the street description, may be null (then the address is invalid)
     * @param location the map location, may be null (then the address is invalid)
     */
    public DeliveryAddress(String street, Location location) {
        this.street = street;
        this.location = location;
    }

    /** @return the street description */
    public String getStreet() {
        return street;
    }

    /** @return the map location */
    public Location getLocation() {
        return location;
    }

    /** @return {@code true} if the street is not blank and the location is set */
    public boolean isValid() {
        return street != null && !street.trim().isEmpty() && location != null;
    }

    @Override
    public String toString() {
        return street + " " + location;
    }
}
