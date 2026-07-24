package com.dtvi.Pet.Ecommerce.dto.user;

import com.dtvi.Pet.Ecommerce.dto.AddressDto;
import com.dtvi.Pet.Ecommerce.entity.User;

import java.time.LocalDateTime;

public record UserProfileResponse(
        Long id,
        String name,
        String email,
        String phoneNumber,
        String role,
        boolean enabled,
        AddressDto address,
        LocalDateTime createdAt
) {
    public static UserProfileResponse from(User user, AddressDto address) {
        return new UserProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getRole().name(),
                user.isEnabled(),
                address,
                user.getCreatedAt()
        );
    }
}
