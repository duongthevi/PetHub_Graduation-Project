package com.dtvi.Pet.Ecommerce.service.impl;

import com.dtvi.Pet.Ecommerce.entity.RefreshToken;
import com.dtvi.Pet.Ecommerce.entity.User;
import com.dtvi.Pet.Ecommerce.exception.RefreshTokenException;
import com.dtvi.Pet.Ecommerce.repository.RefreshTokenRepo;
import com.dtvi.Pet.Ecommerce.service.interf.RefreshTokenService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
@Transactional
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private static final int TOKEN_BYTES = 32;
    private static final int MAX_DEVICE_INFO_LENGTH = 500;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenRepo refreshTokenRepo;

    @Value("${app.jwt.refresh-token-expiration-days:30}")
    private long refreshTokenExpirationDays;

    @PostConstruct
    private void validateConfiguration() {
        if (refreshTokenExpirationDays <= 0) {
            throw new IllegalStateException(
                    "app.jwt.refresh-token-expiration-days must be greater than zero"
            );
        }
    }

    @Override
    public IssuedRefreshToken issue(User user, String deviceInfo) {
        String rawToken = generateRawToken();
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(hash(rawToken))
                .expiresAt(LocalDateTime.now().plusDays(refreshTokenExpirationDays))
                .deviceInfo(normalizeDeviceInfo(deviceInfo))
                .build();

        RefreshToken savedToken = refreshTokenRepo.save(refreshToken);
        return new IssuedRefreshToken(rawToken, user, savedToken);
    }

    @Override
    public IssuedRefreshToken rotate(String rawRefreshToken, String deviceInfo) {
        String token = requireToken(rawRefreshToken);
        RefreshToken currentToken = findForUpdate(token);

        if (currentToken.isRevoked()) {
            throw new RefreshTokenException(
                    "REFRESH_TOKEN_REVOKED",
                    "Refresh token has been revoked"
            );
        }
        if (currentToken.isExpired()) {
            throw new RefreshTokenException(
                    "REFRESH_TOKEN_EXPIRED",
                    "Refresh token has expired"
            );
        }
        if (!currentToken.getUser().isEnabled()) {
            refreshTokenRepo.revokeAllActiveTokensByUserId(
                    currentToken.getUser().getId(),
                    LocalDateTime.now()
            );
            throw new RefreshTokenException(
                    "ACCOUNT_DISABLED",
                    "User account is disabled"
            );
        }

        IssuedRefreshToken issuedToken = issue(currentToken.getUser(), deviceInfo);
        currentToken.revoke(issuedToken.tokenEntity());
        refreshTokenRepo.save(currentToken);
        return issuedToken;
    }

    @Override
    public void revoke(String rawRefreshToken) {
        if (!StringUtils.hasText(rawRefreshToken)) {
            return;
        }

        refreshTokenRepo.findForUpdateByTokenHash(hash(rawRefreshToken.trim()))
                .ifPresent(refreshToken -> {
                    if (!refreshToken.isRevoked()) {
                        refreshToken.revoke(null);
                        refreshTokenRepo.save(refreshToken);
                    }
                });
    }

    @Override
    public void revokeAll(String rawRefreshToken) {
        String token = requireToken(rawRefreshToken);
        RefreshToken currentToken = findForUpdate(token);
        if (currentToken.isRevoked()) {
            throw new RefreshTokenException(
                    "REFRESH_TOKEN_REVOKED",
                    "Refresh token has been revoked"
            );
        }
        if (currentToken.isExpired()) {
            throw new RefreshTokenException(
                    "REFRESH_TOKEN_EXPIRED",
                    "Refresh token has expired"
            );
        }
        refreshTokenRepo.revokeAllActiveTokensByUserId(
                currentToken.getUser().getId(),
                LocalDateTime.now()
        );
    }

    private RefreshToken findForUpdate(String rawRefreshToken) {
        return refreshTokenRepo.findForUpdateByTokenHash(hash(rawRefreshToken))
                .orElseThrow(() -> new RefreshTokenException(
                        "REFRESH_TOKEN_INVALID",
                        "Refresh token is invalid"
                ));
    }

    private String requireToken(String rawRefreshToken) {
        if (!StringUtils.hasText(rawRefreshToken)) {
            throw new RefreshTokenException(
                    "REFRESH_TOKEN_MISSING",
                    "Refresh token cookie is missing"
            );
        }
        return rawRefreshToken.trim();
    }

    private String generateRawToken() {
        byte[] randomBytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private String normalizeDeviceInfo(String deviceInfo) {
        if (!StringUtils.hasText(deviceInfo)) {
            return null;
        }
        String normalized = deviceInfo.trim();
        return normalized.length() <= MAX_DEVICE_INFO_LENGTH
                ? normalized
                : normalized.substring(0, MAX_DEVICE_INFO_LENGTH);
    }
}
