package com.dtvi.Pet.Ecommerce.service;

import com.dtvi.Pet.Ecommerce.entity.Order;
import com.dtvi.Pet.Ecommerce.entity.User;
import com.dtvi.Pet.Ecommerce.enums.OrderStatus;
import com.dtvi.Pet.Ecommerce.enums.PaymentMethod;
import com.dtvi.Pet.Ecommerce.enums.PaymentStatus;
import com.dtvi.Pet.Ecommerce.exception.BusinessException;
import com.dtvi.Pet.Ecommerce.repository.CartRepo;
import com.dtvi.Pet.Ecommerce.repository.OrderRepo;
import com.dtvi.Pet.Ecommerce.repository.PaymentRepo;
import com.dtvi.Pet.Ecommerce.repository.ProductRepo;
import com.dtvi.Pet.Ecommerce.repository.UserRepo;
import com.dtvi.Pet.Ecommerce.service.impl.CouponService;
import com.dtvi.Pet.Ecommerce.service.impl.InventoryTransactionService;
import com.dtvi.Pet.Ecommerce.service.impl.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServicePaymentStateTests {

    @Mock
    private CartRepo cartRepo;

    @Mock
    private ProductRepo productRepo;

    @Mock
    private OrderRepo orderRepo;

    @Mock
    private PaymentRepo paymentRepo;

    @Mock
    private CouponService couponService;

    @Mock
    private UserRepo userRepo;

    @Mock
    private InventoryTransactionService inventoryTransactionService;

    private OrderService service;

    @BeforeEach
    void setUp() {
        service = new OrderService(
                cartRepo,
                productRepo,
                orderRepo,
                paymentRepo,
                couponService,
                userRepo,
                inventoryTransactionService
        );
    }

    @Test
    void successfulVnpayPaymentAutomaticallyConfirmsOrder() {
        Order order = newOrder(
                OrderStatus.PENDING_PAYMENT,
                PaymentStatus.PENDING,
                PaymentMethod.VNPAY
        );
        order.setPaymentExpiresAt(LocalDateTime.now().plusMinutes(15));
        when(orderRepo.findForUpdateById(order.getId())).thenReturn(Optional.of(order));

        service.markVnpayPaid(order);

        assertEquals(PaymentStatus.PAID, order.getPaymentStatus());
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
        assertNotNull(order.getPaidAt());
        assertNull(order.getPaymentExpiresAt());
        verify(orderRepo).save(order);
    }

    @Test
    void paidOrderCannotBeCancelledByCustomer() {
        Order order = newOrder(
                OrderStatus.PENDING,
                PaymentStatus.PAID,
                PaymentMethod.VNPAY
        );
        when(orderRepo.findForUpdateById(order.getId())).thenReturn(Optional.of(order));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.cancelByUser(order.getUser(), order.getId())
        );

        assertEquals("PAID_ORDER_REFUND_REQUIRED", exception.getCode());
        assertEquals(OrderStatus.PENDING, order.getStatus());
        assertEquals(PaymentStatus.PAID, order.getPaymentStatus());
        verify(orderRepo, never()).save(order);
    }

    @Test
    void failedVnpayPaymentAttemptOnlyRecordsReasonWithoutCancellingOrder() {
        Order order = newOrder(
                OrderStatus.PENDING_PAYMENT,
                PaymentStatus.PENDING,
                PaymentMethod.VNPAY
        );
        when(orderRepo.findForUpdateById(order.getId())).thenReturn(Optional.of(order));

        service.recordOnlinePaymentAttemptFailed(order, "VNPay rejected the payment");

        assertEquals(OrderStatus.PENDING_PAYMENT, order.getStatus());
        assertEquals(PaymentStatus.PENDING, order.getPaymentStatus());
        assertEquals("VNPay rejected the payment", order.getAdminMessage());
        verify(orderRepo).save(order);
    }

    @Test
    void adminCannotManuallyPromoteUnpaidVnpayOrder() {
        Order order = newOrder(
                OrderStatus.PENDING_PAYMENT,
                PaymentStatus.PENDING,
                PaymentMethod.VNPAY
        );
        when(orderRepo.findForUpdateById(order.getId())).thenReturn(Optional.of(order));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.updateStatus(order.getId(), OrderStatus.PENDING, null)
        );

        assertEquals("INVALID_ORDER_TRANSITION", exception.getCode());
        assertEquals(OrderStatus.PENDING_PAYMENT, order.getStatus());
    }

    private Order newOrder(
            OrderStatus orderStatus,
            PaymentStatus paymentStatus,
            PaymentMethod paymentMethod
    ) {
        User user = new User();
        user.setId(1L);

        Order order = new Order();
        order.setId(10L);
        order.setUser(user);
        order.setStatus(orderStatus);
        order.setPaymentStatus(paymentStatus);
        order.setPaymentMethod(paymentMethod);
        order.setOrderItemList(new ArrayList<>());
        return order;
    }
}
