package com.dtvi.Pet.Ecommerce.service.impl;

import com.dtvi.Pet.Ecommerce.dto.refund.RefundCommand;
import com.dtvi.Pet.Ecommerce.dto.refund.RefundResponse;
import com.dtvi.Pet.Ecommerce.entity.User;
import com.dtvi.Pet.Ecommerce.exception.BusinessException;
import com.dtvi.Pet.Ecommerce.payment.RefundGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RefundProcessingService {

    private final RefundService refundService;
    private final RefundGateway refundGateway;

    public RefundResponse process(
            Long refundId,
            User admin
    ) {
        RefundCommand command =
                refundService.beginProcessing(refundId, admin);

        try {
            RefundGateway.RefundGatewayResult result =
                    refundGateway.refund(command);

            if (!result.confirmed()) {
                refundService.processingFailed(
                        refundId,
                        "Provider rejected refund: " + result.responseCode(),
                        result.responseCode(),
                        result.rawResponse()
                );

                if ("REFUND_API_NOT_CONFIGURED".equals(result.responseCode())) {
                        throw BusinessException.conflict(
                                "REFUND_API_NOT_CONFIGURED",
                                "Refund API has not been configured"
                        );
                }

                throw BusinessException.conflict(
                        "REFUND_PROVIDER_REJECTED",
                        "VNPay did not confirm the refund"
                );
            }

            if (result.providerRefundReference() == null
                    || result.providerRefundReference().isBlank()) {
                refundService.processingFailed(
                        refundId,
                        "Provider returned no refund reference",
                        result.responseCode(),
                        result.rawResponse()
                );

                throw BusinessException.conflict(
                        "REFUND_REFERENCE_MISSING",
                        "Provider did not return a refund transaction reference"
                );
            }

            return refundService.complete(
                    refundId,
                    result.providerRefundReference(),
                    result.responseCode(),
                    result.rawResponse()
            );
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            refundService.processingUncertain(
                    refundId,
                    exception.getMessage()
            );

            throw BusinessException.conflict(
                    "REFUND_PROVIDER_ERROR",
                    "Unable to confirm refund with provider"
            );
        }
    }
}
