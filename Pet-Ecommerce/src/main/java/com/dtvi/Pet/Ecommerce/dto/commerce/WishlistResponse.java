package com.dtvi.Pet.Ecommerce.dto.commerce;

import java.time.LocalDateTime;

public record WishlistResponse(
        Long id,
        ProductSummary product,
        LocalDateTime createdAt
) {
}
