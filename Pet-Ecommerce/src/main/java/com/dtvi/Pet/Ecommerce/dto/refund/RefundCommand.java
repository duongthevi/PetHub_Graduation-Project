package com.dtvi.Pet.Ecommerce.dto.refund;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RefundCommand(
        Long refundId,
        String providerRequestId,
        Long orderId,
        String orderCode,
        String transactionRef,
        String providerTransactionNo,
        BigDecimal amount,
        LocalDateTime providerPaidAt
) {
}
