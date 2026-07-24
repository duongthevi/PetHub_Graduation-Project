package com.dtvi.Pet.Ecommerce.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank
        @Pattern(regexp = "^(\\+84|0)\\d{9,10}$", message = "phoneNumber is invalid")
        String phoneNumber
) {
}
