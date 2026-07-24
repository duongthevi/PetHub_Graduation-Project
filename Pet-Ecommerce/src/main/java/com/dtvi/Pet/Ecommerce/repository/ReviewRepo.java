package com.dtvi.Pet.Ecommerce.repository;

import com.dtvi.Pet.Ecommerce.entity.Review;
import com.dtvi.Pet.Ecommerce.enums.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface ReviewRepo extends JpaRepository<Review, Long> {

    Page<Review> findByProductIdAndStatus(Long productId, ReviewStatus status, Pageable pageable);

    Optional<Review> findByUserIdAndProductId(Long userId, Long productId);

    @Query("select coalesce(avg(r.rating), 0) from Review r where r.product.id = :productId and r.status = :status")
    BigDecimal averageRating(@Param("productId") Long productId, @Param("status") ReviewStatus status);

    long countByProductIdAndStatus(Long productId, ReviewStatus status);
}
