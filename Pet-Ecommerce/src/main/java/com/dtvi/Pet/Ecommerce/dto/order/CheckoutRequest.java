package com.dtvi.Pet.Ecommerce.dto.order;

import com.dtvi.Pet.Ecommerce.enums.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CheckoutRequest(
        @NotBlank @Size(max = 120) String recipientName,
        @NotBlank
        @Pattern(regexp = "^(\\+84|0)\\d{9,10}$", message = "recipientPhone is invalid")
        String recipientPhone,
        @NotBlank @Size(max = 255) String street,
        @Size(max = 120) String ward,
        @NotBlank @Size(max = 120) String district,
        @NotBlank @Size(max = 120) String city,
        @NotBlank @Size(max = 120) String country,
        @Size(max = 50) String couponCode,
        @NotNull PaymentMethod paymentMethod,
        @Size(max = 1000) String note
) {
}
