package com.dtvi.Pet.Ecommerce.dto.commerce;

import java.math.BigDecimal;
import java.util.Map;

public record ReviewStatisticsResponse(
        long totalReviews,
        BigDecimal averageRating,
        long pendingReplies,
        long repliedReviews,
        Map<Integer, Long> ratingDistribution
) {
}
