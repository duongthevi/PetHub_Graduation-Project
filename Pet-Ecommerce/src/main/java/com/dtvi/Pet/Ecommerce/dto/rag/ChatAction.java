package com.dtvi.Pet.Ecommerce.dto.rag;

public record ChatAction(
        ChatActionType type,
        Long productId,
        String productName,
        Integer quantity,
        String confirmationMessage
) {
}