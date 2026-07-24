package com.dtvi.Pet.Ecommerce.service;

import com.dtvi.Pet.Ecommerce.entity.Coupon;
import com.dtvi.Pet.Ecommerce.enums.CouponType;
import com.dtvi.Pet.Ecommerce.service.impl.CouponService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CouponServiceTests {

    private final CouponService couponService = new CouponService(null, null);

    @Test
    void percentageDiscountHonorsMaximum() {
        Coupon coupon = new Coupon();
        coupon.setType(CouponType.PERCENT);
        coupon.setValue(new BigDecimal("20"));
        coupon.setMaximumDiscount(new BigDecimal("50000"));

        BigDecimal discount = couponService.calculateDiscount(coupon, new BigDecimal("400000"));

        assertEquals(new BigDecimal("50000"), discount);
    }

    @Test
    void fixedDiscountNeverMakesTotalNegative() {
        Coupon coupon = new Coupon();
        coupon.setType(CouponType.FIXED);
        coupon.setValue(new BigDecimal("200000"));

        BigDecimal discount = couponService.calculateDiscount(coupon, new BigDecimal("120000"));

        assertEquals(new BigDecimal("120000"), discount);
    }
}
