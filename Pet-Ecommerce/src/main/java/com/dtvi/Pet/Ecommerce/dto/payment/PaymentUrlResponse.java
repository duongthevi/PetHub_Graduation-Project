package com.dtvi.Pet.Ecommerce.dto.payment;

public record PaymentUrlResponse(
        Long orderId,
        String orderCode,
        String paymentUrl,
        String expiresAt
) {
}
