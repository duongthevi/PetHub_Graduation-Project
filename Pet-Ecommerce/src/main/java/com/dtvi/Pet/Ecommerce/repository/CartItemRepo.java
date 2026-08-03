package com.dtvi.Pet.Ecommerce.repository;

import com.dtvi.Pet.Ecommerce.entity.CartItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CartItemRepo extends JpaRepository<CartItem, Long> {
    Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select item
            from CartItem item
            join fetch item.cart
            join fetch item.product
            where item.cart.id = :cartId
            and item.product.id = :productId
            """)
    Optional<CartItem> findForUpdateByCartAndProduct(
            @Param("cartId") Long cartId,
            @Param("productId") Long productId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select item
            from CartItem item
            join fetch item.cart cart
            join fetch cart.user
            join fetch item.product
            where item.id = :id
            """)
    Optional<CartItem> findForUpdateById(
            @Param("id") Long id
    );
}
