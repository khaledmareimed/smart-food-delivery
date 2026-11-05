package com.fooddelivery.service;

import java.math.BigDecimal;

/**
 * Cash is always accepted once the driver has the money.
 */
public class CashRegister implements PaymentGateway {

    @Override
    public boolean process(String orderId, BigDecimal amount) {
        System.out.println("Cash received for " + orderId + ": " + amount);
        return true;
    }
}
