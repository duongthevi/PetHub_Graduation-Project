package com.dtvi.Pet.Ecommerce.service.impl;

import com.dtvi.Pet.Ecommerce.dto.AddressDto;
import com.dtvi.Pet.Ecommerce.dto.user.ChangePasswordRequest;
import com.dtvi.Pet.Ecommerce.dto.user.ProfileUpdateRequest;
import com.dtvi.Pet.Ecommerce.dto.user.UserProfileResponse;
import com.dtvi.Pet.Ecommerce.entity.User;
import com.dtvi.Pet.Ecommerce.enums.UserRole;
import com.dtvi.Pet.Ecommerce.exception.BusinessException;
import com.dtvi.Pet.Ecommerce.exception.NotFoundException;
import com.dtvi.Pet.Ecommerce.mapper.EntityDtoMapper;
import com.dtvi.Pet.Ecommerce.repository.RefreshTokenRepo;
import com.dtvi.Pet.Ecommerce.repository.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class ProfileService {

    private final UserRepo userRepo;
    private final RefreshTokenRepo refreshTokenRepo;
    private final PasswordEncoder passwordEncoder;
    private final EntityDtoMapper mapper;

    @Transactional(readOnly = true)
    public UserProfileResponse get(User user) {
        User managed = getManaged(user.getId());
        AddressDto address = managed.getAddress() == null
                ? null
                : mapper.mapAddressToDtoBasic(managed.getAddress());
        return UserProfileResponse.from(managed, address);
    }

    public UserProfileResponse update(User user, ProfileUpdateRequest request) {
        user = getManaged(user.getId());
        if (userRepo.existsByPhoneNumberAndIdNot(request.phoneNumber(), user.getId())) {
            throw BusinessException.conflict("PHONE_EXISTS", "Phone number is already registered");
        }
        user.setName(request.name().trim());
        user.setPhoneNumber(request.phoneNumber().trim());
        return get(userRepo.save(user));
    }

    public void changePassword(User user, ChangePasswordRequest request) {
        user = getManaged(user.getId());
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw BusinessException.badRequest("CURRENT_PASSWORD_INVALID", "Current password is incorrect");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw BusinessException.badRequest(
                    "PASSWORD_UNCHANGED",
                    "New password must be different from the current password"
            );
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepo.save(user);
        refreshTokenRepo.revokeAllActiveTokensByUserId(user.getId(), LocalDateTime.now());
    }

    public UserProfileResponse setEnabled(Long actorId, Long userId, boolean enabled) {
        if (!enabled && actorId.equals(userId)) {
            throw BusinessException.conflict(
                    "ADMIN_SELF_DISABLE_FORBIDDEN",
                    "Administrators cannot disable their own account"
            );
        }
        User user = userRepo.findByIdForUpdate(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (!enabled && user.getRole() == UserRole.ADMIN
                && userRepo.countByRoleAndEnabledTrue(UserRole.ADMIN) <= 1) {
            throw BusinessException.conflict(
                    "LAST_ADMIN_DISABLE_FORBIDDEN",
                    "The last active administrator cannot be disabled"
            );
        }
        user.setEnabled(enabled);
        userRepo.save(user);
        if (!enabled) {
            refreshTokenRepo.revokeAllActiveTokensByUserId(userId, LocalDateTime.now());
        }
        return get(user);
    }

    private User getManaged(Long userId) {
        return userRepo.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }
}
