package com.dtvi.Pet.Ecommerce.dto.rag;

import com.dtvi.Pet.Ecommerce.entity.KnowledgeDocument;

import java.time.LocalDateTime;

public record KnowledgeDocumentResponse(
        Long id,
        String title,
        String sourceUrl,
        boolean active,
        int chunkCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static KnowledgeDocumentResponse from(KnowledgeDocument document) {
        return new KnowledgeDocumentResponse(
                document.getId(),
                document.getTitle(),
                document.getSourceUrl(),
                document.isActive(),
                document.getChunks().size(),
                document.getCreatedAt(),
                document.getUpdatedAt()
        );
    }
}
