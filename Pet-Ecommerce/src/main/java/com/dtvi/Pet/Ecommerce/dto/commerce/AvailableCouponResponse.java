package com.dtvi.Pet.Ecommerce.dto.commerce;

import com.dtvi.Pet.Ecommerce.enums.CouponType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AvailableCouponResponse(
        String code,
        String description,
        CouponType type,
        BigDecimal value,
        BigDecimal minimumOrderValue,
        BigDecimal maximumDiscount,
        BigDecimal discount,
        BigDecimal total,
        LocalDateTime expiresAt
) {
}