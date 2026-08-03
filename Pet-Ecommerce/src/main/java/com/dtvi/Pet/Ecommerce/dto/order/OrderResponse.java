package com.dtvi.Pet.Ecommerce.dto.order;

import com.dtvi.Pet.Ecommerce.entity.Order;
import com.dtvi.Pet.Ecommerce.enums.OrderStatus;
import com.dtvi.Pet.Ecommerce.enums.PaymentMethod;
import com.dtvi.Pet.Ecommerce.enums.PaymentStatus;
import com.dtvi.Pet.Ecommerce.enums.RefundStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

public record OrderResponse(
        Long id,
        String orderCode,
        Long userId,
        String customerName,
        List<OrderItemResponse> items,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        BigDecimal shippingFee,
        BigDecimal total,
        String couponCode,
        OrderStatus status,
        PaymentMethod paymentMethod,
        PaymentStatus paymentStatus,
        RefundStatus refundStatus,
        String recipientName,
        String recipientPhone,
        String shippingAddress,
        String note,
        String adminMessage,
        LocalDateTime createdAt,
        LocalDateTime paidAt,
        LocalDateTime cancelledAt,
        LocalDateTime paymentExpiresAt
) {
    public static OrderResponse from(Order order) {
        String address = String.join(", ",
                values(order.getShippingStreet(), order.getShippingWard(), order.getShippingDistrict(),
                        order.getShippingCity(), order.getShippingCountry())
        );
        return new OrderResponse(
                order.getId(),
                order.getOrderCode(),
                order.getUser() == null ? null : order.getUser().getId(),
                order.getUser() == null ? null : order.getUser().getName(),
                order.getOrderItemList() == null
                        ? List.of()
                        : order.getOrderItemList().stream().map(OrderItemResponse::from).toList(),
                order.getSubtotal(),
                order.getDiscountAmount(),
                order.getShippingFee(),
                order.getTotalPrice(),
                order.getCoupon() == null ? null : order.getCoupon().getCode(),
                order.getStatus(),
                order.getPaymentMethod(),
                order.getPaymentStatus(),
                order.getRefundRequest() == null
                        ? null : order.getRefundRequest().getStatus(),
                order.getRecipientName(),
                order.getRecipientPhone(),
                address,
                order.getNote(),
                order.getAdminMessage(),
                order.getCreatedAt(),
                order.getPaidAt(),
                order.getCancelledAt(),
                order.getPaymentExpiresAt()
        );
    }

    private static String[] values(String... values) {
        return Arrays.stream(values)
                .filter(value -> value != null && !value.isBlank())
                .toArray(String[]::new);
    }
}
