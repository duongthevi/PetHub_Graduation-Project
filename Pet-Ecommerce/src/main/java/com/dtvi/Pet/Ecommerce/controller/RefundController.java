package com.dtvi.Pet.Ecommerce.controller;

import com.dtvi.Pet.Ecommerce.dto.ApiResponse;
import com.dtvi.Pet.Ecommerce.dto.refund.CreateRefundRequest;
import com.dtvi.Pet.Ecommerce.dto.refund.RefundResponse;
import com.dtvi.Pet.Ecommerce.security.AuthUser;
import com.dtvi.Pet.Ecommerce.service.impl.RefundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@RestController
@RequestMapping("/refunds")
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.refund",
        name = "enabled",
        havingValue = "true"
)
public class RefundController {

    private final RefundService refundService;

    @PostMapping("/orders/{orderId}")
    public ApiResponse<RefundResponse> request(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long orderId,
            @Valid @RequestBody CreateRefundRequest request
    ) {
        return ApiResponse.success(
                "Refund requested",
                refundService.request(
                        authUser.getUser(),
                        orderId,
                        request
                )
        );
    }

    @GetMapping("/my")
    public ApiResponse<Page<RefundResponse>> getMyRefunds(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.success(
                "Refunds retrieved",
                refundService.getMyRefunds(authUser.getUser(), page, size)
        );
    }
}
