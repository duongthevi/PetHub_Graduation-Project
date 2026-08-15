package com.dtvi.Pet.Ecommerce.dto.commerce;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminReviewReplyRequest(
        @NotBlank(message = "Reply is required")
        @Size(max = 2000, message = "Reply must not exceed 2000 characters")
        String reply
) {
}
