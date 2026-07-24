package com.dtvi.Pet.Ecommerce.dto.commerce;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(
        Long id,
        List<CartItemResponse> items,
        int totalItems,
        BigDecimal subtotal
) {
}
