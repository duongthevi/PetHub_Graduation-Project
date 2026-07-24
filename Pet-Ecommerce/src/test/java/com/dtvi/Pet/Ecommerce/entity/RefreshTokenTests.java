package com.dtvi.Pet.Ecommerce.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RefreshTokenTests {

    @Test
    void tokenIsActiveBeforeExpirationAndRevocation() {
        RefreshToken refreshToken = RefreshToken.builder()
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        assertTrue(refreshToken.isActive());
        assertFalse(refreshToken.isExpired());
        assertFalse(refreshToken.isRevoked());
    }

    @Test
    void revokeMarksTokenAndLinksReplacement() {
        RefreshToken refreshToken = RefreshToken.builder()
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();
        RefreshToken replacement = RefreshToken.builder()
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        refreshToken.revoke(replacement);

        assertTrue(refreshToken.isRevoked());
        assertFalse(refreshToken.isActive());
        assertSame(replacement, refreshToken.getReplacedByToken());
    }

    @Test
    void expiredTokenIsNotActive() {
        RefreshToken refreshToken = RefreshToken.builder()
                .expiresAt(LocalDateTime.now().minusSeconds(1))
                .build();

        assertTrue(refreshToken.isExpired());
        assertFalse(refreshToken.isActive());
    }
}
