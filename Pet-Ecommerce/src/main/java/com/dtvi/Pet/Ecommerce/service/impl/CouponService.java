package com.dtvi.Pet.Ecommerce.service.impl;

import com.dtvi.Pet.Ecommerce.dto.commerce.CouponRequest;
import com.dtvi.Pet.Ecommerce.dto.commerce.CouponResponse;
import com.dtvi.Pet.Ecommerce.dto.commerce.CouponValidationResponse;
import com.dtvi.Pet.Ecommerce.entity.Coupon;
import com.dtvi.Pet.Ecommerce.entity.CouponUsage;
import com.dtvi.Pet.Ecommerce.entity.Order;
import com.dtvi.Pet.Ecommerce.entity.User;
import com.dtvi.Pet.Ecommerce.enums.CouponType;
import com.dtvi.Pet.Ecommerce.exception.BusinessException;
import com.dtvi.Pet.Ecommerce.exception.NotFoundException;
import com.dtvi.Pet.Ecommerce.repository.CouponRepo;
import com.dtvi.Pet.Ecommerce.repository.CouponUsageRepo;
import com.dtvi.Pet.Ecommerce.dto.commerce.AvailableCouponResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class CouponService {

    private final CouponRepo couponRepo;
    private final CouponUsageRepo couponUsageRepo;

    @Transactional(readOnly = true)
    public List<CouponResponse> getAll() {
        return couponRepo.findAllByOrderByCreatedAtDesc().stream().map(CouponResponse::from).toList();
    }

    public CouponResponse create(CouponRequest request) {
        validateRequest(request);
        String code = normalize(request.code());
        if (couponRepo.existsByCodeIgnoreCase(code)) {
            throw BusinessException.conflict("COUPON_CODE_EXISTS", "Coupon code already exists");
        }
        Coupon coupon = new Coupon();
        apply(coupon, request);
        return CouponResponse.from(couponRepo.save(coupon));
    }

    public CouponResponse update(Long id, CouponRequest request) {
        validateRequest(request);
        Coupon coupon = couponRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Coupon not found"));
        couponRepo.findByCodeIgnoreCase(request.code()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw BusinessException.conflict("COUPON_CODE_EXISTS", "Coupon code already exists");
            }
        });
        apply(coupon, request);
        return CouponResponse.from(couponRepo.save(coupon));
    }

    public void delete(Long id) {
        Coupon coupon = couponRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Coupon not found"));
        coupon.setActive(false);
        couponRepo.save(coupon);
    }

    @Transactional(readOnly = true)
    public CouponValidationResponse validate(
            String code,
            BigDecimal subtotal,
            User user
    ) {
        Coupon coupon = couponRepo
                .findByCodeIgnoreCase(normalize(code))
                .orElseThrow(() ->
                        new NotFoundException("Coupon not found")
                );

        validateCouponRules(coupon, subtotal, user);

        BigDecimal discount =
                calculateDiscount(coupon, subtotal);

        return new CouponValidationResponse(
                coupon.getCode(),
                subtotal,
                discount,
                subtotal.subtract(discount)
        );
    }

    public Coupon requireValid(
            String code,
            BigDecimal subtotal,
            User user
    ) {
        Coupon coupon = couponRepo
                .findByCodeForUpdate(normalize(code))
                .orElseThrow(() ->
                        new NotFoundException("Coupon not found")
                );

        validateCouponRules(coupon, subtotal, user);

        return coupon;
    }

    private void validateCouponRules(
            Coupon coupon,
            BigDecimal subtotal,
            User user
    ) {
        if (subtotal == null || subtotal.signum() < 0) {
            throw BusinessException.badRequest(
                    "INVALID_SUBTOTAL",
                    "Subtotal must not be negative"
            );
        }

        LocalDateTime now = LocalDateTime.now();

        if (!coupon.isActive()
                || now.isBefore(coupon.getStartsAt())
                || now.isAfter(coupon.getExpiresAt())) {
            throw BusinessException.badRequest(
                    "COUPON_NOT_ACTIVE",
                    "Coupon is not active"
            );
        }

        if (subtotal.compareTo(
                coupon.getMinimumOrderValue()
        ) < 0) {
            throw BusinessException.badRequest(
                    "MINIMUM_ORDER_NOT_REACHED",
                    "Order does not reach the coupon's minimum value"
            );
        }

        if (coupon.getUsageLimit() != null
                && coupon.getUsedCount()
                >= coupon.getUsageLimit()) {
            throw BusinessException.badRequest(
                    "COUPON_USAGE_EXHAUSTED",
                    "Coupon usage limit has been reached"
            );
        }

        long userUsage =
                couponUsageRepo.countByCouponIdAndUserId(
                        coupon.getId(),
                        user.getId()
                );

        if (userUsage >= coupon.getPerUserLimit()) {
            throw BusinessException.badRequest(
                    "COUPON_USER_LIMIT",
                    "You have reached the coupon usage limit"
            );
        }
    }

    public BigDecimal calculateDiscount(Coupon coupon, BigDecimal subtotal) {
        BigDecimal discount = coupon.getType() == CouponType.PERCENT
                ? subtotal.multiply(coupon.getValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                : coupon.getValue();
        if (coupon.getMaximumDiscount() != null) {
            discount = discount.min(coupon.getMaximumDiscount());
        }
        return discount.min(subtotal).max(BigDecimal.ZERO).setScale(0, RoundingMode.HALF_UP);
    }

    public void recordUsage(Coupon coupon, User user, Order order) {
        coupon.setUsedCount(coupon.getUsedCount() + 1);
        couponRepo.save(coupon);
        CouponUsage usage = new CouponUsage();
        usage.setCoupon(coupon);
        usage.setUser(user);
        usage.setOrder(order);
        couponUsageRepo.save(usage);
    }

    public void releaseUsage(Order order) {
        couponUsageRepo.findByOrderId(order.getId()).ifPresent(usage -> {
            Coupon coupon = usage.getCoupon();
            coupon.setUsedCount(Math.max(0, coupon.getUsedCount() - 1));
            couponRepo.save(coupon);
            couponUsageRepo.delete(usage);
        });
    }

    private void validateRequest(CouponRequest request) {
        if (!request.expiresAt().isAfter(request.startsAt())) {
            throw BusinessException.badRequest("INVALID_COUPON_PERIOD", "Coupon expiry must be after its start");
        }
        if (request.type() == CouponType.PERCENT && request.value().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw BusinessException.badRequest("INVALID_COUPON_VALUE", "Percentage discount must not exceed 100");
        }
    }

    private void apply(Coupon coupon, CouponRequest request) {
        coupon.setCode(normalize(request.code()));
        coupon.setDescription(request.description());
        coupon.setType(request.type());
        coupon.setValue(request.value());
        coupon.setMinimumOrderValue(request.minimumOrderValue());
        coupon.setMaximumDiscount(request.maximumDiscount());
        coupon.setStartsAt(request.startsAt());
        coupon.setExpiresAt(request.expiresAt());
        coupon.setUsageLimit(request.usageLimit());
        coupon.setPerUserLimit(request.perUserLimit());
        coupon.setActive(request.active());
    }

    private String normalize(String code) {
        if (code == null || code.isBlank()) {
            throw BusinessException.badRequest("COUPON_CODE_REQUIRED", "Coupon code is required");
        }
        return code.trim().toUpperCase(Locale.ROOT);
    }

    @Transactional(readOnly = true)
    public List<AvailableCouponResponse> getAvailableCoupons(
            BigDecimal subtotal,
            User user
    ) {
        if (subtotal == null || subtotal.signum() < 0) {
            throw BusinessException.badRequest(
                    "INVALID_SUBTOTAL",
                    "Subtotal must not be negative"
            );
        }

        LocalDateTime now = LocalDateTime.now();

        return couponRepo
                .findAvailableCoupons(subtotal, now)
                .stream()
                .filter(coupon -> {
                    long userUsage =
                            couponUsageRepo
                                    .countByCouponIdAndUserId(
                                            coupon.getId(),
                                            user.getId()
                                    );

                    return userUsage < coupon.getPerUserLimit();
                })
                .map(coupon -> {
                    BigDecimal discount =
                            calculateDiscount(coupon, subtotal);

                    return new AvailableCouponResponse(
                            coupon.getCode(),
                            coupon.getDescription(),
                            coupon.getType(),
                            coupon.getValue(),
                            coupon.getMinimumOrderValue(),
                            coupon.getMaximumDiscount(),
                            discount,
                            subtotal.subtract(discount),
                            coupon.getExpiresAt()
                    );
                })
                .toList();
    }
}
