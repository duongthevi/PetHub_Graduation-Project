package com.dtvi.Pet.Ecommerce.controller;

import com.dtvi.Pet.Ecommerce.dto.ApiResponse;
import com.dtvi.Pet.Ecommerce.dto.commerce.WishlistResponse;
import com.dtvi.Pet.Ecommerce.security.AuthUser;
import com.dtvi.Pet.Ecommerce.service.impl.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping
    public ApiResponse<List<WishlistResponse>> getWishlist(@AuthenticationPrincipal AuthUser authUser) {
        return ApiResponse.success("Wishlist retrieved", wishlistService.getWishlist(authUser.getUser()));
    }

    @PostMapping("/{productId}")
    public ApiResponse<WishlistResponse> add(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long productId
    ) {
        return ApiResponse.success("Product added to wishlist", wishlistService.add(authUser.getUser(), productId));
    }

    @DeleteMapping("/{productId}")
    public ApiResponse<Void> remove(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long productId
    ) {
        wishlistService.remove(authUser.getUser(), productId);
        return ApiResponse.success("Product removed from wishlist");
    }
}
