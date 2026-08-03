package com.dtvi.Pet.Ecommerce.repository;

import com.dtvi.Pet.Ecommerce.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface CouponRepo extends JpaRepository<Coupon, Long> {
    Optional<Coupon> findByCodeIgnoreCase(String code);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select coupon from Coupon coupon where upper(coupon.code) = upper(:code)")
    Optional<Coupon> findByCodeForUpdate(@Param("code") String code);
    boolean existsByCodeIgnoreCase(String code);
    List<Coupon> findAllByOrderByCreatedAtDesc();

    @Query("""
            select coupon
            from Coupon coupon
            where coupon.active = true
            and coupon.startsAt <= :now
            and coupon.expiresAt >= :now
            and coupon.minimumOrderValue <= :subtotal
            and (
                    coupon.usageLimit is null
                    or coupon.usedCount < coupon.usageLimit
            )
            order by coupon.expiresAt asc
            """)
    List<Coupon> findAvailableCoupons(
            @Param("subtotal") BigDecimal subtotal,
            @Param("now") LocalDateTime now
    );
}
