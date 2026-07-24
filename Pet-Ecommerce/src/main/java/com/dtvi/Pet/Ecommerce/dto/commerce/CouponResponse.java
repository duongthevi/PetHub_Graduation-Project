package com.dtvi.Pet.Ecommerce.dto.commerce;

import com.dtvi.Pet.Ecommerce.entity.Coupon;
import com.dtvi.Pet.Ecommerce.enums.CouponType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CouponResponse(
        Long id,
        String code,
        String description,
        CouponType type,
        BigDecimal value,
        BigDecimal minimumOrderValue,
        BigDecimal maximumDiscount,
        LocalDateTime startsAt,
        LocalDateTime expiresAt,
        Integer usageLimit,
        int usedCount,
        int perUserLimit,
        boolean active
) {
    public static CouponResponse from(Coupon coupon) {
        return new CouponResponse(
                coupon.getId(),
                coupon.getCode(),
                coupon.getDescription(),
                coupon.getType(),
                coupon.getValue(),
                coupon.getMinimumOrderValue(),
                coupon.getMaximumDiscount(),
                coupon.getStartsAt(),
                coupon.getExpiresAt(),
                coupon.getUsageLimit(),
                coupon.getUsedCount(),
                coupon.getPerUserLimit(),
                coupon.isActive()
        );
    }
}
