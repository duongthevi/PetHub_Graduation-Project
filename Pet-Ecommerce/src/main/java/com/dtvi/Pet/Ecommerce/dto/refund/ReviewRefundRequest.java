package com.dtvi.Pet.Ecommerce.dto.refund;

import jakarta.validation.constraints.Size;

public record ReviewRefundRequest(
        @Size(max = 2000)
        String adminNote
) {
}
