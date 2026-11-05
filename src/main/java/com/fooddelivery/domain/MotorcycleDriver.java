package com.fooddelivery.domain;

/**
 * Motorcycles do medium distance trips (max 10 km), small orders.
 */
public class MotorcycleDriver extends Driver {

    public MotorcycleDriver(String id, String name, Location location, double speedKmh) {
        super(id, name, location, speedKmh);
    }

    @Override
    public String getType() {
        return "MOTORCYCLE";
    }

    @Override
    public double getMaxDistanceKm() {
        return 10;
    }

    @Override
    public boolean canDeliver(Order order) {
        if (order.getItemCount() >= 10) {
            return false;
        }
        return deliveryDistance(order) <= 10;
    }
}
