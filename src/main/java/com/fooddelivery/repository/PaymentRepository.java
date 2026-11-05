package com.fooddelivery.repository;

import com.fooddelivery.domain.Payment;

import java.util.List;

public interface PaymentRepository {

    void save(Payment payment);

    List<Payment> findByOrderId(String orderId);

    int count();
}
