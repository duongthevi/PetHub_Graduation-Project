package com.dtvi.Pet.Ecommerce.dto.rag;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRequest(
        @Size(max = 36) String conversationId,
        @NotBlank @Size(max = 2000) String question
) {
}
