package com.dtvi.Pet.Ecommerce.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse {

    private final String accessToken;

    @Builder.Default
    private final String tokenType = "Bearer";

    private final long expiresIn;
    private final AuthUserResponse user;
}
