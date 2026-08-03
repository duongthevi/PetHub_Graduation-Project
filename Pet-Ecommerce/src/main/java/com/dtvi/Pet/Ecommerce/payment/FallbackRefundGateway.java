package com.dtvi.Pet.Ecommerce.payment;

import com.dtvi.Pet.Ecommerce.dto.refund.RefundCommand;

public class FallbackRefundGateway implements RefundGateway {

    @Override
    public RefundGatewayResult refund(RefundCommand command) {
        return new RefundGatewayResult(
                false,
                null,
                "REFUND_API_NOT_CONFIGURED",
                "Refund gateway is not configured for this environment"
        );
    }
}
