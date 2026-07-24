package com.dtvi.Pet.Ecommerce.service.impl;

import com.dtvi.Pet.Ecommerce.dto.commerce.*;
import com.dtvi.Pet.Ecommerce.entity.Cart;
import com.dtvi.Pet.Ecommerce.entity.CartItem;
import com.dtvi.Pet.Ecommerce.entity.Product;
import com.dtvi.Pet.Ecommerce.entity.User;
import com.dtvi.Pet.Ecommerce.exception.BusinessException;
import com.dtvi.Pet.Ecommerce.exception.NotFoundException;
import com.dtvi.Pet.Ecommerce.repository.CartItemRepo;
import com.dtvi.Pet.Ecommerce.repository.CartRepo;
import com.dtvi.Pet.Ecommerce.repository.ProductRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CartService {

    private final CartRepo cartRepo;
    private final CartItemRepo cartItemRepo;
    private final ProductRepo productRepo;

    @Transactional(readOnly = true)
    public CartResponse getCart(User user) {
        return cartRepo.findWithItemsByUserId(user.getId())
                .map(this::toResponse)
                .orElse(new CartResponse(null, List.of(), 0, BigDecimal.ZERO));
    }

    public CartResponse addItem(User user, CartItemRequest request) {
        Product product = getPurchasableProduct(request.productId());
        Cart cart = cartRepo.findByUserId(user.getId()).orElseGet(() -> createCart(user));
        CartItem item = cartItemRepo.findByCartIdAndProductId(cart.getId(), product.getId())
                .orElseGet(() -> createItem(cart, product));

        int requestedQuantity = item.getQuantity() + request.quantity();
        ensureStock(product, requestedQuantity);
        item.setQuantity(requestedQuantity);
        cartItemRepo.save(item);
        return loadCart(user.getId());
    }

    public CartResponse updateItem(User user, Long itemId, int quantity) {
        if (quantity < 1 || quantity > 99) {
            throw BusinessException.badRequest("INVALID_QUANTITY", "Quantity must be between 1 and 99");
        }
        CartItem item = getOwnedItem(user, itemId);
        Product product = item.getProduct();
        if (!product.isActive()) {
            throw BusinessException.conflict("PRODUCT_UNAVAILABLE", "Product is no longer available");
        }
        ensureStock(product, quantity);
        item.setQuantity(quantity);
        return loadCart(user.getId());
    }

    public CartResponse removeItem(User user, Long itemId) {
        cartItemRepo.delete(getOwnedItem(user, itemId));
        return loadCart(user.getId());
    }

    public void clear(User user) {
        cartRepo.findWithItemsByUserId(user.getId()).ifPresent(cart -> {
            cart.getItems().clear();
            cartRepo.save(cart);
        });
    }

    private Cart createCart(User user) {
        Cart cart = new Cart();
        cart.setUser(user);
        return cartRepo.save(cart);
    }

    private CartItem createItem(Cart cart, Product product) {
        CartItem item = new CartItem();
        item.setCart(cart);
        item.setProduct(product);
        item.setQuantity(0);
        return item;
    }

    private CartItem getOwnedItem(User user, Long itemId) {
        CartItem item = cartItemRepo.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Cart item not found"));
        if (!item.getCart().getUser().getId().equals(user.getId())) {
            throw BusinessException.forbidden("CART_ITEM_FORBIDDEN", "Cart item does not belong to this user");
        }
        return item;
    }

    private Product getPurchasableProduct(Long productId) {
        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found"));
        if (!product.isActive()) {
            throw BusinessException.conflict("PRODUCT_UNAVAILABLE", "Product is no longer available");
        }
        return product;
    }

    private void ensureStock(Product product, int quantity) {
        if (quantity > product.getQuantity()) {
            throw BusinessException.conflict(
                    "INSUFFICIENT_STOCK",
                    "Only " + product.getQuantity() + " unit(s) remain for " + product.getName()
            );
        }
    }

    private CartResponse loadCart(Long userId) {
        return cartRepo.findWithItemsByUserId(userId)
                .map(this::toResponse)
                .orElse(new CartResponse(null, List.of(), 0, BigDecimal.ZERO));
    }

    private CartResponse toResponse(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream()
                .map(item -> {
                    Product product = item.getProduct();
                    return new CartItemResponse(
                            item.getId(),
                            ProductSummary.from(product),
                            item.getQuantity(),
                            product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())),
                            product.isActive() && product.getQuantity() >= item.getQuantity()
                    );
                })
                .toList();
        int totalItems = items.stream().mapToInt(CartItemResponse::quantity).sum();
        BigDecimal subtotal = items.stream()
                .map(CartItemResponse::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartResponse(cart.getId(), items, totalItems, subtotal);
    }
}
