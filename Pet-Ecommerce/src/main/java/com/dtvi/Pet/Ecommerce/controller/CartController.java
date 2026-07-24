package com.dtvi.Pet.Ecommerce.controller;

import com.dtvi.Pet.Ecommerce.dto.ApiResponse;
import com.dtvi.Pet.Ecommerce.dto.commerce.CartItemRequest;
import com.dtvi.Pet.Ecommerce.dto.commerce.CartResponse;
import com.dtvi.Pet.Ecommerce.security.AuthUser;
import com.dtvi.Pet.Ecommerce.service.impl.CartService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
@Validated
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ApiResponse<CartResponse> getCart(@AuthenticationPrincipal AuthUser authUser) {
        return ApiResponse.success("Cart retrieved", cartService.getCart(authUser.getUser()));
    }

    @PostMapping("/items")
    public ApiResponse<CartResponse> addItem(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody CartItemRequest request
    ) {
        return ApiResponse.success("Product added to cart", cartService.addItem(authUser.getUser(), request));
    }

    @PatchMapping("/items/{itemId}")
    public ApiResponse<CartResponse> updateItem(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long itemId,
            @RequestParam @Min(1) @Max(99) int quantity
    ) {
        return ApiResponse.success(
                "Cart item updated",
                cartService.updateItem(authUser.getUser(), itemId, quantity)
        );
    }

    @DeleteMapping("/items/{itemId}")
    public ApiResponse<CartResponse> removeItem(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long itemId
    ) {
        return ApiResponse.success(
                "Cart item removed",
                cartService.removeItem(authUser.getUser(), itemId)
        );
    }

    @DeleteMapping
    public ApiResponse<Void> clear(@AuthenticationPrincipal AuthUser authUser) {
        cartService.clear(authUser.getUser());
        return ApiResponse.success("Cart cleared");
    }
}
