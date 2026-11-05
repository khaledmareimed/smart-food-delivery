package com.fooddelivery.domain;

/**
 * Cars can take large orders (10 items or more) and any distance.
 */
public class CarDriver extends Driver {

    public CarDriver(String id, String name, Location location, double speedKmh) {
        super(id, name, location, speedKmh);
    }

    @Override
    public String getType() {
        return "CAR";
    }

    @Override
    public boolean canDeliver(Order order) {
        return true;
    }
}
