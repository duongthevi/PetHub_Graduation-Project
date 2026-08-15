package com.dtvi.Pet.Ecommerce.dto.commerce;

import com.dtvi.Pet.Ecommerce.entity.Review;

import java.time.LocalDateTime;

public record ReviewResponse(
        Long id,
        Long userId,
        String userName,
        Long productId,
        String productName,
        int rating,
        String comment,
        boolean verifiedPurchase,
        String adminReply,
        String repliedByName,
        LocalDateTime repliedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ReviewResponse from(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getUser().getId(),
                review.getUser().getName(),
                review.getProduct().getId(),
                review.getProduct().getName(),
                review.getRating(),
                review.getComment(),
                review.isVerifiedPurchase(),
                review.getAdminReply(),
                review.getRepliedByName(),
                review.getRepliedAt(),
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }
}
