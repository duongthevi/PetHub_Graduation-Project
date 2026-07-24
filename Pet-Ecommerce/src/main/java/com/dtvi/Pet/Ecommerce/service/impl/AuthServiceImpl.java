package com.dtvi.Pet.Ecommerce.service.impl;

import com.dtvi.Pet.Ecommerce.dto.AuthResponse;
import com.dtvi.Pet.Ecommerce.dto.AuthUserResponse;
import com.dtvi.Pet.Ecommerce.dto.LoginRequest;
import com.dtvi.Pet.Ecommerce.entity.User;
import com.dtvi.Pet.Ecommerce.exception.InvalidCredentialsException;
import com.dtvi.Pet.Ecommerce.repository.UserRepo;
import com.dtvi.Pet.Ecommerce.security.JwtUtils;
import com.dtvi.Pet.Ecommerce.service.interf.AuthService;
import com.dtvi.Pet.Ecommerce.service.interf.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final RefreshTokenService refreshTokenService;

    @Override
    public AuthSession login(LoginRequest loginRequest, String deviceInfo) {
        String email = loginRequest.getEmail().trim().toLowerCase(Locale.ROOT);
        User user = userRepo.findByEmail(email)
                .orElseThrow(this::invalidCredentials);

        if (!user.isEnabled() || !passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw invalidCredentials();
        }

        RefreshTokenService.IssuedRefreshToken refreshToken =
                refreshTokenService.issue(user, deviceInfo);

        return buildSession(user, refreshToken.rawToken());
    }

    @Override
    public AuthSession refresh(String rawRefreshToken, String deviceInfo) {
        RefreshTokenService.IssuedRefreshToken refreshToken =
                refreshTokenService.rotate(rawRefreshToken, deviceInfo);
        return buildSession(refreshToken.user(), refreshToken.rawToken());
    }

    @Override
    public void logout(String rawRefreshToken) {
        refreshTokenService.revoke(rawRefreshToken);
    }

    @Override
    public void logoutAll(String rawRefreshToken) {
        refreshTokenService.revokeAll(rawRefreshToken);
    }

    private AuthSession buildSession(User user, String rawRefreshToken) {
        AuthResponse response = AuthResponse.builder()
                .accessToken(jwtUtils.generateToken(user))
                .expiresIn(jwtUtils.getAccessTokenExpirationSeconds())
                .user(AuthUserResponse.from(user))
                .build();
        return new AuthSession(response, rawRefreshToken);
    }

    private InvalidCredentialsException invalidCredentials() {
        return new InvalidCredentialsException("Email or password is incorrect");
    }
}
