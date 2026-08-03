package com.dtvi.Pet.Ecommerce.dto.inventory;

import com.dtvi.Pet.Ecommerce.entity.InventoryTransaction;
import com.dtvi.Pet.Ecommerce.enums.InventoryTransactionType;

import java.time.LocalDateTime;

public record InventoryTransactionResponse(
        Long id,
        Long productId,
        String productName,
        Long orderId,
        String orderCode,
        InventoryTransactionType transactionType,
        int quantityChanged,
        Integer balanceAfter,
        String referenceNumber,
        Long createdById,
        String createdByName,
        LocalDateTime createdAt
) {
    public static InventoryTransactionResponse from(
            InventoryTransaction transaction
    ) {
        return new InventoryTransactionResponse(
                transaction.getId(),
                transaction.getProduct().getId(),
                transaction.getProduct().getName(),
                transaction.getOrder() == null
                        ? null : transaction.getOrder().getId(),
                transaction.getOrder() == null
                        ? null : transaction.getOrder().getOrderCode(),
                transaction.getTransactionType(),
                transaction.getQuantityChanged(),
                transaction.getBalanceAfter(),
                transaction.getReferenceNumber(),
                transaction.getCreatedBy() == null
                        ? null : transaction.getCreatedBy().getId(),
                transaction.getCreatedBy() == null
                        ? null : transaction.getCreatedBy().getName(),
                transaction.getCreatedAt()
        );
    }
}