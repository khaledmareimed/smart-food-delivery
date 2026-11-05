package com.fooddelivery.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.fooddelivery.domain.DeliveryAddress;
import com.fooddelivery.domain.Location;
import com.fooddelivery.domain.MenuItem;
import com.fooddelivery.domain.Order;
import com.fooddelivery.domain.OrderItem;
import com.fooddelivery.domain.OrderStatus;
import com.fooddelivery.domain.Payment;
import com.fooddelivery.domain.Restaurant;
import com.fooddelivery.domain.Role;
import com.fooddelivery.domain.User;
import com.fooddelivery.exception.PaymentException;
import com.fooddelivery.exception.ResourceNotFoundException;
import com.fooddelivery.repository.OrderRepository;
import com.fooddelivery.repository.PaymentRepository;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** US4.4 cash payment with mocked payment gateway. */
class PaymentServiceTest {

    OrderRepository orders;
    PaymentRepository payments;
    PaymentGateway gateway;
    PaymentService service;
    Order order;

    @BeforeEach
    void setUp() {
        orders = mock(OrderRepository.class);
        payments = mock(PaymentRepository.class);
        gateway = mock(PaymentGateway.class);
        service = new PaymentService(orders, payments, gateway);
        MenuItem m = new MenuItem("m1", "A", new BigDecimal("20.00"), true);
        order = new Order("ORD-1", new User("u1", "a", "h", Role.CUSTOMER),
                new Restaurant("r1", "R", true), Collections.singletonList(new OrderItem(m, 2)),
                new DeliveryAddress("Main", new Location(1, 1)));
        order.setTotal(new BigDecimal("52.50"));
        when(orders.findById("ORD-1")).thenReturn(Optional.of(order));
        when(gateway.process(anyString(), any(BigDecimal.class))).thenReturn(true);
    }

    private Payment lastSavedPayment() {
        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(payments).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void validCashPaymentIsProcessed() {
        Payment p = service.payCash("ORD-1", new BigDecimal("60.00"));
        assertEquals("PAID", p.getStatus());
        assertEquals("CASH", p.getMethod());
        assertEquals("ORD-1", p.getOrderId());
        assertEquals("PAY-1", p.getId());
        assertEquals(new BigDecimal("60.00"), p.getAmount());
        assertEquals(0, new BigDecimal("7.50").compareTo(p.getChange()));
        assertEquals("PAID", order.getPaymentStatus());
        verify(gateway).process("ORD-1", new BigDecimal("52.50"));
        verify(orders).save(order);
        assertSame(p, lastSavedPayment());
    }

    @Test
    void exactAmountIsAccepted() {
        Payment p = service.payCash("ORD-1", new BigDecimal("52.50"));
        assertEquals(0, BigDecimal.ZERO.compareTo(p.getChange()));
    }

    @Test
    void notEnoughMoneyIsRejectedAndRecorded() {
        assertThrows(PaymentException.class, () -> service.payCash("ORD-1", new BigDecimal("50")));
        assertEquals("REJECTED", lastSavedPayment().getStatus());
        assertEquals("UNPAID", order.getPaymentStatus());
        verify(gateway, never()).process(anyString(), any(BigDecimal.class));
    }

    @Test
    void zeroNegativeOrNullAmountRejected() {
        assertThrows(PaymentException.class, () -> service.payCash("ORD-1", BigDecimal.ZERO));
        assertThrows(PaymentException.class, () -> service.payCash("ORD-1", new BigDecimal("-5")));
        assertThrows(PaymentException.class, () -> service.payCash("ORD-1", null));
        verify(payments, times(3)).save(any(Payment.class));
    }

    @Test
    void gatewayFailureIsRecorded() {
        when(gateway.process(anyString(), any(BigDecimal.class))).thenReturn(false);
        assertThrows(PaymentException.class, () -> service.payCash("ORD-1", new BigDecimal("100")));
        assertEquals("FAILED", lastSavedPayment().getStatus());
        assertEquals("UNPAID", order.getPaymentStatus());
    }

    @Test
    void cannotPayTwiceOrCancelledOrUnknown() {
        order.setPaymentStatus("PAID");
        assertThrows(PaymentException.class, () -> service.payCash("ORD-1", new BigDecimal("100")));
        order.setPaymentStatus("UNPAID");
        order.setStatus(OrderStatus.CANCELLED);
        assertThrows(PaymentException.class, () -> service.payCash("ORD-1", new BigDecimal("100")));
        assertThrows(ResourceNotFoundException.class, () -> service.payCash("X", BigDecimal.TEN));
        verify(payments, never()).save(any(Payment.class));
    }

    @Test
    void paymentsListedFromRepository() {
        when(payments.findByOrderId("ORD-1")).thenReturn(Collections.<Payment>emptyList());
        assertTrue(service.getPayments("ORD-1").isEmpty());
    }

    @Test
    void cashRegisterAlwaysAccepts() {
        assertTrue(new CashRegister().process("ORD-1", BigDecimal.TEN));
    }
}
