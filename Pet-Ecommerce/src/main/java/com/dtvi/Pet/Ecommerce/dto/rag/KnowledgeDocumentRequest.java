package com.dtvi.Pet.Ecommerce.dto.rag;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record KnowledgeDocumentRequest(
        @NotBlank @Size(max = 255) String title,
        @Size(max = 1000) String sourceUrl,
        @NotBlank @Size(max = 200000) String content
) {
}
