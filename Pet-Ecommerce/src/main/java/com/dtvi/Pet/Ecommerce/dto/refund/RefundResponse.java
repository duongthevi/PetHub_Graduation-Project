package com.dtvi.Pet.Ecommerce.dto.refund;

import com.dtvi.Pet.Ecommerce.entity.RefundRequest;
import com.dtvi.Pet.Ecommerce.enums.RefundStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RefundResponse(
        Long id,
        Long orderId,
        String orderCode,
        Long requestedById,
        String requestedByName,
        BigDecimal amount,
        String reason,
        RefundStatus status,
        String providerRefundRef,
        String adminNote,
        String lastError,
        LocalDateTime createdAt,
        LocalDateTime reviewedAt,
        LocalDateTime completedAt
) {
    public static RefundResponse from(
            RefundRequest refund
    ) {
        return new RefundResponse(
                refund.getId(),
                refund.getOrder().getId(),
                refund.getOrder().getOrderCode(),
                refund.getRequestedBy().getId(),
                refund.getRequestedBy().getName(),
                refund.getAmount(),
                refund.getReason(),
                refund.getStatus(),
                refund.getProviderRefundRef(),
                refund.getAdminNote(),
                refund.getLastError(),
                refund.getCreatedAt(),
                refund.getReviewedAt(),
                refund.getCompletedAt()
        );
    }
}
