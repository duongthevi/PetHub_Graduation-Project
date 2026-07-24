package com.dtvi.Pet.Ecommerce.dto;

import com.dtvi.Pet.Ecommerce.entity.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthUserResponse {

    private final Long id;
    private final String name;
    private final String role;

    public static AuthUserResponse from(User user) {
        return AuthUserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .role(user.getRole().name())
                .build();
    }
}
