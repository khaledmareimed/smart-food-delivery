package com.fooddelivery.service;

import java.math.BigDecimal;

/**
 * Where money goes. For cash this is the cash register. Mocked in tests.
 */
public interface PaymentGateway {

    boolean process(String orderId, BigDecimal amount);
}
