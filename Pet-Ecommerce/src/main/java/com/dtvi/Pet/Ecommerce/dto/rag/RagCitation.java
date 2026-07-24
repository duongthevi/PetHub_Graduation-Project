package com.dtvi.Pet.Ecommerce.dto.rag;

public record RagCitation(
        Long documentId,
        Long chunkId,
        String title,
        String sourceUrl,
        double score
) {
}
