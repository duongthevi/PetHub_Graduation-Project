package com.dtvi.Pet.Ecommerce.dto.commerce;

import com.dtvi.Pet.Ecommerce.enums.CouponType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CouponRequest(
        @NotBlank @Size(max = 50) String code,
        @Size(max = 255) String description,
        @NotNull CouponType type,
        @NotNull @DecimalMin("0.01") BigDecimal value,
        @NotNull @DecimalMin("0.00") BigDecimal minimumOrderValue,
        @DecimalMin("0.00") BigDecimal maximumDiscount,
        @NotNull LocalDateTime startsAt,
        @NotNull LocalDateTime expiresAt,
        @Positive Integer usageLimit,
        @Min(1) int perUserLimit,
        boolean active
) {
}
