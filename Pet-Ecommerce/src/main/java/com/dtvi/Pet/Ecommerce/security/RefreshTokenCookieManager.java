package com.dtvi.Pet.Ecommerce.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

@Component
public class RefreshTokenCookieManager {

    private static final String COOKIE_PATH = "/auth";

    private final String cookieName;
    private final boolean secure;
    private final String sameSite;
    private final Duration maxAge;

    public RefreshTokenCookieManager(
            @Value("${app.jwt.refresh-cookie-name:refreshToken}") String cookieName,
            @Value("${app.jwt.refresh-cookie-secure:false}") boolean secure,
            @Value("${app.jwt.refresh-cookie-same-site:Lax}") String sameSite,
            @Value("${app.jwt.refresh-token-expiration-days:30}") long expirationDays
    ) {
        if (cookieName == null || cookieName.isBlank()) {
            throw new IllegalStateException("app.jwt.refresh-cookie-name must not be blank");
        }
        if (expirationDays <= 0) {
            throw new IllegalStateException(
                    "app.jwt.refresh-token-expiration-days must be greater than zero"
            );
        }
        this.cookieName = cookieName;
        this.secure = secure;
        this.sameSite = normalizeSameSite(sameSite);
        if ("None".equals(this.sameSite) && !secure) {
            throw new IllegalStateException(
                    "SameSite=None requires app.jwt.refresh-cookie-secure=true"
            );
        }
        this.maxAge = Duration.ofDays(expirationDays);
    }

    public ResponseCookie create(String rawRefreshToken) {
        return baseCookie(rawRefreshToken)
                .maxAge(maxAge)
                .build();
    }

    public ResponseCookie clear() {
        return baseCookie("")
                .maxAge(Duration.ZERO)
                .build();
    }

    public Optional<String> read(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(cookie -> cookieName.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> value != null && !value.isBlank())
                .findFirst();
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
        return ResponseCookie.from(cookieName, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path(COOKIE_PATH);
    }

    private String normalizeSameSite(String configuredValue) {
        if (configuredValue == null) {
            return "Lax";
        }
        return switch (configuredValue.trim().toLowerCase()) {
            case "strict" -> "Strict";
            case "none" -> "None";
            default -> "Lax";
        };
    }
}
