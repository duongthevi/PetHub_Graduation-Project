package com.dtvi.Pet.Ecommerce.dto.payment;

public record PaymentResultResponse(
        boolean signatureValid,
        boolean successful,
        String orderCode,
        String responseCode,
        String message
) {
}
