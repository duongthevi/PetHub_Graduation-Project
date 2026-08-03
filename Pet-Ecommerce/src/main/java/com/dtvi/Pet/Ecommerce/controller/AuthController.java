package com.dtvi.Pet.Ecommerce.controller;


import com.dtvi.Pet.Ecommerce.dto.AuthResponse;
import com.dtvi.Pet.Ecommerce.dto.LoginRequest;
import com.dtvi.Pet.Ecommerce.dto.Response;
import com.dtvi.Pet.Ecommerce.dto.UserDto;
import com.dtvi.Pet.Ecommerce.dto.ApiResponse;
import com.dtvi.Pet.Ecommerce.dto.auth.ForgotPasswordRequest;
import com.dtvi.Pet.Ecommerce.dto.auth.ResetPasswordRequest;
import com.dtvi.Pet.Ecommerce.security.RefreshTokenCookieManager;
import com.dtvi.Pet.Ecommerce.service.interf.AuthService;
import com.dtvi.Pet.Ecommerce.service.interf.UserService;
import com.dtvi.Pet.Ecommerce.service.impl.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.dtvi.Pet.Ecommerce.service.impl.RequestRateLimiter;
import java.time.Duration;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

        private final UserService userService;
        private final AuthService authService;
        private final RefreshTokenCookieManager refreshTokenCookieManager;
        private final PasswordResetService passwordResetService;
        private final RequestRateLimiter requestRateLimiter;

        @PostMapping("/register")
        public ResponseEntity<Response> registerUser(@Valid @RequestBody UserDto registrationRequest) {
                return ResponseEntity.ok(userService.registerUser(registrationRequest));
        }

        @PostMapping("/login")
        public ResponseEntity<AuthResponse> loginUser(
                @Valid @RequestBody LoginRequest loginRequest,
                HttpServletRequest request
        ) {
                AuthService.AuthSession session = authService.login(
                        loginRequest,
                        request.getHeader(HttpHeaders.USER_AGENT)
                );
                return authResponse(session);
        }

        @PostMapping("/refresh")
        public ResponseEntity<AuthResponse> refresh(HttpServletRequest request) {
                AuthService.AuthSession session = authService.refresh(
                        refreshTokenCookieManager.read(request).orElse(null),
                        request.getHeader(HttpHeaders.USER_AGENT)
                );
                return authResponse(session);
        }

        @PostMapping("/logout")
        public ResponseEntity<Void> logout(HttpServletRequest request) {
                authService.logout(refreshTokenCookieManager.read(request).orElse(null));
                return ResponseEntity.noContent()
                        .header(
                                HttpHeaders.SET_COOKIE,
                                refreshTokenCookieManager.clear().toString()
                        )
                        .build();
        }

        @PostMapping("/logout-all")
        public ResponseEntity<Void> logoutAll(HttpServletRequest request) {
                authService.logoutAll(refreshTokenCookieManager.read(request).orElse(null));
                return ResponseEntity.noContent()
                        .header(
                                HttpHeaders.SET_COOKIE,
                                refreshTokenCookieManager.clear().toString()
                        )
                        .build();
        }

        @PostMapping("/forgot-password")
        public ApiResponse<Void> forgotPassword(
                @Valid @RequestBody ForgotPasswordRequest request,
                HttpServletRequest servletRequest
        ) {
                String email = request.email()
                        .trim()
                        .toLowerCase(java.util.Locale.ROOT);

                String ip = clientIp(servletRequest);

                requestRateLimiter.check(
                        "forgot-ip",
                        ip,
                        10,
                        Duration.ofMinutes(15)
                );

                requestRateLimiter.check(
                        "forgot-email",
                        email,
                        3,
                        Duration.ofMinutes(15)
                );

                passwordResetService.requestReset(email);

                return ApiResponse.success(
                        "If the email exists, password reset instructions have been sent"
                );
        }

        @PostMapping("/reset-password")
        public ApiResponse<Void> resetPassword(
                @Valid @RequestBody ResetPasswordRequest request,
                HttpServletRequest servletRequest
        ) {
                requestRateLimiter.check(
                        "reset-password-ip",
                        clientIp(servletRequest),
                        10,
                        Duration.ofMinutes(15)
                );

                passwordResetService.resetPassword(
                        request.token(),
                        request.newPassword()
                );

                return ApiResponse.success(
                        "Password reset successfully"
                );
        }

        private ResponseEntity<AuthResponse> authResponse(AuthService.AuthSession session) {
                return ResponseEntity.ok()
                        .cacheControl(CacheControl.noStore())
                        .header(HttpHeaders.PRAGMA, "no-cache")
                        .header(
                                HttpHeaders.SET_COOKIE,
                                refreshTokenCookieManager.create(session.refreshToken()).toString()
                        )
                        .body(session.response());
        }

        private String clientIp(HttpServletRequest request) {
                String cloudflareIp =
                        request.getHeader("CF-Connecting-IP");

                if (cloudflareIp != null && !cloudflareIp.isBlank()) {
                        return cloudflareIp.trim();
                }

                String forwarded =
                        request.getHeader("X-Forwarded-For");

                if (forwarded != null && !forwarded.isBlank()) {
                        return forwarded.split(",")[0].trim();
                }

                return request.getRemoteAddr();
        }
}
