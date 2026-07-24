package com.dtvi.Pet.Ecommerce.dto.commerce;

import com.dtvi.Pet.Ecommerce.entity.Product;

import java.math.BigDecimal;

public record ProductSummary(
        Long id,
        String name,
        String sku,
        String imageUrl,
        BigDecimal price,
        int stock,
        boolean active
) {
    public static ProductSummary from(Product product) {
        return new ProductSummary(
                product.getId(),
                product.getName(),
                product.getSku(),
                product.getImageUrl(),
                product.getPrice(),
                product.getQuantity(),
                product.isActive()
        );
    }
}
