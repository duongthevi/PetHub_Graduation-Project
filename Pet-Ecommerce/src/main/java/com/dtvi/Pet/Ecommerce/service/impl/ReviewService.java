package com.dtvi.Pet.Ecommerce.service.impl;

import com.dtvi.Pet.Ecommerce.dto.commerce.ReviewRequest;
import com.dtvi.Pet.Ecommerce.dto.commerce.ReviewResponse;
import com.dtvi.Pet.Ecommerce.dto.commerce.ReviewStatisticsResponse;
import com.dtvi.Pet.Ecommerce.entity.Product;
import com.dtvi.Pet.Ecommerce.entity.Review;
import com.dtvi.Pet.Ecommerce.entity.User;
import com.dtvi.Pet.Ecommerce.enums.OrderStatus;
import com.dtvi.Pet.Ecommerce.enums.ReviewStatus;
import com.dtvi.Pet.Ecommerce.exception.BusinessException;
import com.dtvi.Pet.Ecommerce.exception.NotFoundException;
import com.dtvi.Pet.Ecommerce.repository.OrderItemRepo;
import com.dtvi.Pet.Ecommerce.repository.ProductRepo;
import com.dtvi.Pet.Ecommerce.repository.ReviewRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewService {

    private final ReviewRepo reviewRepo;
    private final ProductRepo productRepo;
    private final OrderItemRepo orderItemRepo;

    @Transactional(readOnly = true)
    public Page<ReviewResponse> getPublished(Long productId, int page, int size) {
        return reviewRepo.findByProductIdAndStatus(
                        productId,
                        ReviewStatus.PUBLISHED,
                        PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 50)),
                                Sort.by(Sort.Direction.DESC, "createdAt"))
                )
                .map(ReviewResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse> getAllForAdmin(int page, int size) {
        return reviewRepo.findByStatus(
                        ReviewStatus.PUBLISHED,
                        PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 100)),
                                Sort.by(Sort.Direction.DESC, "createdAt"))
                )
                .map(ReviewResponse::from);
    }

    @Transactional(readOnly = true)
    public ReviewStatisticsResponse getStatistics() {
        long total = reviewRepo.countByStatus(ReviewStatus.PUBLISHED);
        long pending = reviewRepo.countByStatusAndAdminReplyIsNull(ReviewStatus.PUBLISHED);
        Map<Integer, Long> distribution = new LinkedHashMap<>();
        for (int rating = 5; rating >= 1; rating--) {
            distribution.put(rating, reviewRepo.countByStatusAndRating(ReviewStatus.PUBLISHED, rating));
        }
        BigDecimal average = total == 0
                ? BigDecimal.ZERO
                : distribution.entrySet().stream()
                        .map(entry -> BigDecimal.valueOf((long) entry.getKey() * entry.getValue()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
        return new ReviewStatisticsResponse(total, average, pending, total - pending, distribution);
    }

    public ReviewResponse reply(User admin, Long reviewId, String reply) {
        Review review = reviewRepo.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Review not found"));
        review.setAdminReply(reply.trim());
        review.setRepliedByName(admin.getName());
        review.setRepliedAt(LocalDateTime.now());
        return ReviewResponse.from(reviewRepo.save(review));
    }

    public ReviewResponse create(User user, Long productId, ReviewRequest request) {
        if (reviewRepo.findByUserIdAndProductId(user.getId(), productId).isPresent()) {
            throw BusinessException.conflict("REVIEW_ALREADY_EXISTS", "You have already reviewed this product");
        }
        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found"));
        boolean purchased = orderItemRepo.existsByUserIdAndProductIdAndStatus(
                user.getId(), productId, OrderStatus.DELIVERED
        );
        if (!purchased) {
            throw BusinessException.forbidden(
                    "PURCHASE_REQUIRED",
                    "Only customers with a delivered order may review this product"
            );
        }
        Review review = new Review();
        review.setUser(user);
        review.setProduct(product);
        applyRequest(review, request);
        review.setVerifiedPurchase(true);
        Review saved = reviewRepo.save(review);
        refreshProductRating(product);
        return ReviewResponse.from(saved);
    }

    public ReviewResponse update(User user, Long reviewId, ReviewRequest request) {
        Review review = getOwnedReview(user, reviewId);
        applyRequest(review, request);
        Review saved = reviewRepo.save(review);
        refreshProductRating(review.getProduct());
        return ReviewResponse.from(saved);
    }

    public void delete(User user, Long reviewId, boolean admin) {
        Review review = reviewRepo.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Review not found"));
        if (!admin && !review.getUser().getId().equals(user.getId())) {
            throw BusinessException.forbidden("REVIEW_FORBIDDEN", "You cannot delete another user's review");
        }
        Product product = review.getProduct();
        reviewRepo.delete(review);
        reviewRepo.flush();
        refreshProductRating(product);
    }

    private Review getOwnedReview(User user, Long reviewId) {
        Review review = reviewRepo.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Review not found"));
        if (!review.getUser().getId().equals(user.getId())) {
            throw BusinessException.forbidden("REVIEW_FORBIDDEN", "You cannot edit another user's review");
        }
        return review;
    }

    private void applyRequest(Review review, ReviewRequest request) {
        review.setRating(request.rating());
        review.setComment(request.comment() == null ? null : request.comment().trim());
        review.setStatus(ReviewStatus.PUBLISHED);
    }

    private void refreshProductRating(Product product) {
        long count = reviewRepo.countByProductIdAndStatus(product.getId(), ReviewStatus.PUBLISHED);
        product.setReviewCount(count);
        product.setAverageRating(
                reviewRepo.averageRating(product.getId(), ReviewStatus.PUBLISHED)
                        .setScale(2, RoundingMode.HALF_UP)
        );
        productRepo.save(product);
    }
}
