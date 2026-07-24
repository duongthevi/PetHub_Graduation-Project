package com.dtvi.Pet.Ecommerce.service.impl;

import com.dtvi.Pet.Ecommerce.dto.commerce.ReviewRequest;
import com.dtvi.Pet.Ecommerce.dto.commerce.ReviewResponse;
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
                        PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "createdAt"))
                )
                .map(ReviewResponse::from);
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
