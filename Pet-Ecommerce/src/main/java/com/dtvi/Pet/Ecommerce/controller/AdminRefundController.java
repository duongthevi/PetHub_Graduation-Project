package com.dtvi.Pet.Ecommerce.controller;

import com.dtvi.Pet.Ecommerce.dto.ApiResponse;
import com.dtvi.Pet.Ecommerce.dto.refund.RefundResponse;
import com.dtvi.Pet.Ecommerce.dto.refund.ReviewRefundRequest;
import com.dtvi.Pet.Ecommerce.enums.RefundStatus;
import com.dtvi.Pet.Ecommerce.security.AuthUser;
import com.dtvi.Pet.Ecommerce.service.impl.RefundProcessingService;
import com.dtvi.Pet.Ecommerce.service.impl.RefundService;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@RestController
@RequestMapping("/admin/refunds")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
@ConditionalOnProperty(
        prefix = "app.refund",
        name = "enabled",
        havingValue = "true"
)
public class AdminRefundController {

    private final RefundService refundService;
    private final RefundProcessingService processingService;

    @GetMapping
    public ApiResponse<Page<RefundResponse>> getAll(
            @RequestParam(required = false) RefundStatus status,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(
                "Refunds retrieved",
                refundService.getAllRefunds(page, size, status, q)
        );
    }

    @PatchMapping("/{id}/approve")
    public ApiResponse<RefundResponse> approve(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long id,
            @Valid @RequestBody(required = false) ReviewRefundRequest request
    ) {
        return ApiResponse.success(
                "Refund approved",
                refundService.approve(
                        id,
                        authUser.getUser(),
                        request == null ? null : request.adminNote()
                )
        );
    }

    @PatchMapping("/{id}/reject")
    public ApiResponse<RefundResponse> reject(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long id,
            @Valid @RequestBody(required = false) ReviewRefundRequest request
    ) {
        return ApiResponse.success(
                "Refund rejected",
                refundService.reject(
                        id,
                        authUser.getUser(),
                        request == null ? null : request.adminNote()
                )
        );
    }

    @PostMapping("/{id}/process")
    public ApiResponse<RefundResponse> process(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long id
    ) {
        return ApiResponse.success(
                "Refund completed",
                processingService.process(
                        id,
                        authUser.getUser()
                )
        );
    }
}
