package com.dtvi.Pet.Ecommerce.dto.payment;

public record PaymentResultResponse(
        boolean signatureValid,
        boolean successful,
        boolean pending,
        Long orderId,
        String orderCode,
        String transactionRef,
        String responseCode,
        String message
) {

    public PaymentResultResponse(
            boolean signatureValid,
            boolean successful,
            String transactionRef,
            String responseCode,
            String message
    ) {
        this(
                signatureValid,
                successful,
                false,
                null,
                null,
                transactionRef,
                responseCode,
                message
        );
    }
}
