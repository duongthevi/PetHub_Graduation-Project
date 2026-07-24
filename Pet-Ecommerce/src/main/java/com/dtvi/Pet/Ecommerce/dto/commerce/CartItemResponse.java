package com.dtvi.Pet.Ecommerce.dto.commerce;

import java.math.BigDecimal;

public record CartItemResponse(
        Long id,
        ProductSummary product,
        int quantity,
        BigDecimal lineTotal,
        boolean available
) {
}
