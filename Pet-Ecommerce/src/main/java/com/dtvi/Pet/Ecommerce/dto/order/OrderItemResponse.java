package com.dtvi.Pet.Ecommerce.dto.order;

import com.dtvi.Pet.Ecommerce.entity.OrderItem;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long id,
        Long productId,
        String productName,
        String productSku,
        String productImageUrl,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {
    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getProduct() == null ? null : item.getProduct().getId(),
                item.getProductName() != null ? item.getProductName()
                        : item.getProduct() == null ? null : item.getProduct().getName(),
                item.getProductSku(),
                item.getProductImageUrl(),
                item.getQuantity(),
                item.getUnitPrice() != null ? item.getUnitPrice()
                        : item.getProduct() == null ? null : item.getProduct().getPrice(),
                item.getLineTotal() != null ? item.getLineTotal() : item.getPrice()
        );
    }
}
