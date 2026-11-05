package com.fooddelivery.domain;

import java.math.BigDecimal;

/**
 * A payment attempt for an order. Only cash for now.
 */
public class Payment {

    private String id;
    private String orderId;
    private String method;
    private BigDecimal amount;
    private BigDecimal change;
    private String status;

    public Payment(String id, String orderId, String method, BigDecimal amount,
                   BigDecimal change, String status) {
        this.id = id;
        this.orderId = orderId;
        this.method = method;
        this.amount = amount;
        this.change = change;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getMethod() {
        return method;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getChange() {
        return change;
    }

    public String getStatus() {
        return status;
    }
}
