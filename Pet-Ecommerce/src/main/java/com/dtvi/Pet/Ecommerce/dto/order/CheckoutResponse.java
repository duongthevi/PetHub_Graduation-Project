package com.dtvi.Pet.Ecommerce.dto.order;

public record CheckoutResponse(
        OrderResponse order,
        boolean requiresOnlinePayment
) {
}
