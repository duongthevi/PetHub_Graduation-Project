package com.dtvi.Pet.Ecommerce.controller;

import com.dtvi.Pet.Ecommerce.dto.Response;
import com.dtvi.Pet.Ecommerce.dto.ApiResponse;
import com.dtvi.Pet.Ecommerce.dto.user.ChangePasswordRequest;
import com.dtvi.Pet.Ecommerce.dto.user.ProfileUpdateRequest;
import com.dtvi.Pet.Ecommerce.dto.user.UserProfileResponse;
import com.dtvi.Pet.Ecommerce.security.AuthUser;
import com.dtvi.Pet.Ecommerce.service.impl.ProfileService;
import com.dtvi.Pet.Ecommerce.service.interf.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final ProfileService profileService;


    @GetMapping("/get-all")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Response> getAllUsers(){
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/my-info")
    public ResponseEntity<Response> getUserInfoAndOrderHistory(){
        return ResponseEntity.ok(userService.getUserInfoAndOrderHistory());
    }

    @GetMapping("/profile")
    public ApiResponse<UserProfileResponse> getProfile(@AuthenticationPrincipal AuthUser authUser) {
        return ApiResponse.success("Profile retrieved", profileService.get(authUser.getUser()));
    }

    @PutMapping("/profile")
    public ApiResponse<UserProfileResponse> updateProfile(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody ProfileUpdateRequest request
    ) {
        return ApiResponse.success(
                "Profile updated",
                profileService.update(authUser.getUser(), request)
        );
    }

    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        profileService.changePassword(authUser.getUser(), request);
        return ApiResponse.success("Password changed; all refresh sessions were revoked");
    }

    @PatchMapping("/admin/{userId}/enabled")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ApiResponse<UserProfileResponse> setEnabled(
            @PathVariable Long userId,
            @RequestParam boolean enabled
    ) {
        return ApiResponse.success("User status updated", profileService.setEnabled(userId, enabled));
    }
}
