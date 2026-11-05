package com.fooddelivery.service;

import com.fooddelivery.domain.Order;
import com.fooddelivery.domain.OrderStatus;
import com.fooddelivery.domain.Payment;
import com.fooddelivery.exception.PaymentException;
import com.fooddelivery.exception.ResourceNotFoundException;
import com.fooddelivery.repository.OrderRepository;
import com.fooddelivery.repository.PaymentRepository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Cash payments (US4.4).
 */
public class PaymentService {

    private OrderRepository orderRepository;
    private PaymentRepository paymentRepository;
    private PaymentGateway gateway;

    public PaymentService(OrderRepository orderRepository, PaymentRepository paymentRepository,
                          PaymentGateway gateway) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.gateway = gateway;
    }

    /**
     * Pays an order in cash. The amount given must cover the total; the change
     * is stored with the payment. Every attempt is recorded with its status.
     */
    public Payment payCash(String orderId, BigDecimal amountGiven) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new PaymentException("Cannot pay a cancelled order");
        }
        if (order.getPaymentStatus().equals("PAID")) {
            throw new PaymentException("Order " + orderId + " is already paid");
        }
        String paymentId = "PAY-" + (paymentRepository.count() + 1);
        if (amountGiven == null || amountGiven.signum() <= 0) {
            paymentRepository.save(new Payment(paymentId, orderId, "CASH", amountGiven, null,
                    "REJECTED"));
            throw new PaymentException("Invalid amount");
        }
        BigDecimal total = order.getTotal();
        if (amountGiven.compareTo(total) < 0) {
            paymentRepository.save(new Payment(paymentId, orderId, "CASH", amountGiven, null,
                    "REJECTED"));
            throw new PaymentException("Amount " + amountGiven + " is less than the total " + total);
        }
        if (!gateway.process(orderId, total)) {
            paymentRepository.save(new Payment(paymentId, orderId, "CASH", amountGiven, null,
                    "FAILED"));
            throw new PaymentException("Payment could not be processed");
        }
        Payment payment = new Payment(paymentId, orderId, "CASH", amountGiven,
                amountGiven.subtract(total), "PAID");
        paymentRepository.save(payment);
        order.setPaymentStatus("PAID");
        orderRepository.save(order);
        return payment;
    }

    public List<Payment> getPayments(String orderId) {
        return paymentRepository.findByOrderId(orderId);
    }
}
