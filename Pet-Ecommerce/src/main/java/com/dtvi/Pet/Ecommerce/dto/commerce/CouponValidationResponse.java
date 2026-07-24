package com.dtvi.Pet.Ecommerce.dto.commerce;

import java.math.BigDecimal;

public record CouponValidationResponse(
        String code,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal total
) {
}
