package com.dtvi.Pet.Ecommerce.controller;

import com.dtvi.Pet.Ecommerce.dto.ApiResponse;
import com.dtvi.Pet.Ecommerce.dto.commerce.CouponRequest;
import com.dtvi.Pet.Ecommerce.dto.commerce.CouponResponse;
import com.dtvi.Pet.Ecommerce.dto.commerce.CouponValidationResponse;
import com.dtvi.Pet.Ecommerce.security.AuthUser;
import com.dtvi.Pet.Ecommerce.service.impl.CouponService;
import com.dtvi.Pet.Ecommerce.dto.commerce.AvailableCouponResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ApiResponse<List<CouponResponse>> getAll() {
        return ApiResponse.success("Coupons retrieved", couponService.getAll());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ApiResponse<CouponResponse> create(@Valid @RequestBody CouponRequest request) {
        return ApiResponse.success("Coupon created", couponService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ApiResponse<CouponResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CouponRequest request
    ) {
        return ApiResponse.success("Coupon updated", couponService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        couponService.delete(id);
        return ApiResponse.success("Coupon deactivated");
    }

    @GetMapping("/validate")
    public ApiResponse<CouponValidationResponse> validate(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam String code,
            @RequestParam BigDecimal subtotal
    ) {
        return ApiResponse.success(
                "Coupon is valid",
                couponService.validate(code, subtotal, authUser.getUser())
        );
    }

    @GetMapping("/available")
    public ApiResponse<List<AvailableCouponResponse>> getAvailableCoupons(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam BigDecimal subtotal
    ) {
        return ApiResponse.success(
                "Available coupons retrieved",
                couponService.getAvailableCoupons(
                        subtotal,
                        authUser.getUser()
                )
        );
    }
}
