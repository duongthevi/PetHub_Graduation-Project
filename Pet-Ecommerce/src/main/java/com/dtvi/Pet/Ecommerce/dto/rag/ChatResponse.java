package com.dtvi.Pet.Ecommerce.dto.rag;

import com.dtvi.Pet.Ecommerce.dto.commerce.ProductSummary;

import java.util.List;

public record ChatResponse(
        String conversationId,
        String answer,
        List<RagCitation> citations,
        List<Long> recommendedProductIds,
        List<ProductSummary> recommendedProducts,
        ChatAction action
) {
}