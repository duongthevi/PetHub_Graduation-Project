package com.dtvi.Pet.Ecommerce.payment;

import com.dtvi.Pet.Ecommerce.dto.refund.RefundCommand;

public interface RefundGateway {

    RefundGatewayResult refund(RefundCommand command);

    record RefundGatewayResult(
            boolean confirmed,
            String providerRefundReference,
            String responseCode,
            String rawResponse
    ) {
    }
}
