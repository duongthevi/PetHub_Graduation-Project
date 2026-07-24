package com.dtvi.Pet.Ecommerce.dto.rag;

import java.util.List;

public record ChatResponse(
        String conversationId,
        String answer,
        List<RagCitation> citations,
        List<Long> recommendedProductIds
) {
}
