package com.dtvi.Pet.Ecommerce.controller;

import com.dtvi.Pet.Ecommerce.dto.ApiResponse;
import com.dtvi.Pet.Ecommerce.dto.order.CheckoutRequest;
import com.dtvi.Pet.Ecommerce.dto.order.CheckoutResponse;
import com.dtvi.Pet.Ecommerce.dto.order.OrderResponse;
import com.dtvi.Pet.Ecommerce.dto.order.OrderStatusRequest;
import com.dtvi.Pet.Ecommerce.enums.OrderStatus;
import com.dtvi.Pet.Ecommerce.security.AuthUser;
import com.dtvi.Pet.Ecommerce.service.impl.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.dtvi.Pet.Ecommerce.exception.BusinessException;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/checkout")
        public ApiResponse<CheckoutResponse> checkout(
                @AuthenticationPrincipal AuthUser authUser,
                @RequestHeader("Idempotency-Key")
                String idempotencyKey,
                @Valid @RequestBody CheckoutRequest request
        ) {
                if (idempotencyKey.isBlank()
                        || idempotencyKey.length() > 64) {
                        throw BusinessException.badRequest(
                                "INVALID_IDEMPOTENCY_KEY",
                                "Idempotency-Key is required and must not exceed 64 characters"
                        );
                }

                return ApiResponse.success(
                        "Order created",
                        orderService.checkout(
                                authUser.getUser(),
                                request,
                                idempotencyKey
                        )
                );
        }

    @GetMapping("/my")
    public ApiResponse<Page<OrderResponse>> getMyOrders(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.success(
                "Orders retrieved",
                orderService.getMyOrders(authUser.getUser(), page, size)
        );
    }

    @GetMapping("/{orderId}")
    public ApiResponse<OrderResponse> getMyOrder(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long orderId
    ) {
        return ApiResponse.success("Order retrieved", orderService.getMyOrder(authUser.getUser(), orderId));
    }

    @PostMapping("/{orderId}/cancel")
    public ApiResponse<OrderResponse> cancel(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long orderId
    ) {
        return ApiResponse.success("Order cancelled", orderService.cancelByUser(authUser.getUser(), orderId));
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ApiResponse<Page<OrderResponse>> getAll(
        @RequestParam(required = false) String q,
        @RequestParam(required = false) OrderStatus status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success("Orders retrieved", orderService.getAllOrders(
                page,
                size,
                q,
                status
        ));
    }

    @PatchMapping("/admin/{orderId}/status")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ApiResponse<OrderResponse> updateStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody OrderStatusRequest request
    ) {
        return ApiResponse.success(
                "Order status updated",
                orderService.updateStatus(orderId, request.status(), request.adminMessage())
        );
    }
}
