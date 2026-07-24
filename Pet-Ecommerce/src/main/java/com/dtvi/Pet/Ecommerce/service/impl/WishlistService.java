package com.dtvi.Pet.Ecommerce.service.impl;

import com.dtvi.Pet.Ecommerce.dto.commerce.ProductSummary;
import com.dtvi.Pet.Ecommerce.dto.commerce.WishlistResponse;
import com.dtvi.Pet.Ecommerce.entity.Product;
import com.dtvi.Pet.Ecommerce.entity.User;
import com.dtvi.Pet.Ecommerce.entity.WishlistItem;
import com.dtvi.Pet.Ecommerce.exception.NotFoundException;
import com.dtvi.Pet.Ecommerce.exception.BusinessException;
import com.dtvi.Pet.Ecommerce.repository.ProductRepo;
import com.dtvi.Pet.Ecommerce.repository.WishlistItemRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class WishlistService {

    private final WishlistItemRepo wishlistItemRepo;
    private final ProductRepo productRepo;

    @Transactional(readOnly = true)
    public List<WishlistResponse> getWishlist(User user) {
        return wishlistItemRepo.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public WishlistResponse add(User user, Long productId) {
        return wishlistItemRepo.findByUserIdAndProductId(user.getId(), productId)
                .map(this::toResponse)
                .orElseGet(() -> {
                    Product product = productRepo.findById(productId)
                            .orElseThrow(() -> new NotFoundException("Product not found"));
                    if (!product.isActive()) {
                        throw BusinessException.conflict(
                                "PRODUCT_UNAVAILABLE",
                                "Product is no longer available"
                        );
                    }
                    WishlistItem item = new WishlistItem();
                    item.setUser(user);
                    item.setProduct(product);
                    return toResponse(wishlistItemRepo.save(item));
                });
    }

    public void remove(User user, Long productId) {
        WishlistItem item = wishlistItemRepo.findByUserIdAndProductId(user.getId(), productId)
                .orElseThrow(() -> new NotFoundException("Wishlist item not found"));
        wishlistItemRepo.delete(item);
    }

    private WishlistResponse toResponse(WishlistItem item) {
        return new WishlistResponse(item.getId(), ProductSummary.from(item.getProduct()), item.getCreatedAt());
    }
}
