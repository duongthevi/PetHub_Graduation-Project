package com.dtvi.Pet.Ecommerce.service.impl;

import com.dtvi.Pet.Ecommerce.entity.PasswordResetToken;
import com.dtvi.Pet.Ecommerce.entity.User;
import com.dtvi.Pet.Ecommerce.exception.BusinessException;
import com.dtvi.Pet.Ecommerce.repository.PasswordResetTokenRepo;
import com.dtvi.Pet.Ecommerce.repository.RefreshTokenRepo;
import com.dtvi.Pet.Ecommerce.repository.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class PasswordResetService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepo userRepo;
    private final PasswordResetTokenRepo tokenRepo;
    private final RefreshTokenRepo refreshTokenRepo;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.mail.from:no-reply@petshop.local}")
    private String mailFrom;

    @Value("${app.mail.reset-url:http://localhost:5173/reset-password}")
    private String resetUrl;

    @Value("${app.mail.reset-token-minutes:15}")
    private long expirationMinutes;

    public void requestReset(String email) {
        if (!mailEnabled) {
            throw BusinessException.badRequest(
                    "EMAIL_NOT_CONFIGURED",
                    "Password reset email has not been configured"
            );
        }
        userRepo.findByEmail(email.trim().toLowerCase(Locale.ROOT)).ifPresent(user -> {
            LocalDateTime now = LocalDateTime.now();
            tokenRepo.invalidateActiveByUserId(user.getId(), now);
            String rawToken = generateToken();
            PasswordResetToken token = new PasswordResetToken();
            token.setUser(user);
            token.setTokenHash(hash(rawToken));
            token.setExpiresAt(now.plusMinutes(expirationMinutes));
            tokenRepo.save(token);
            sendResetEmail(user, rawToken);
        });
    }

    public void resetPassword(String rawToken, String newPassword) {
        PasswordResetToken token = tokenRepo.findByTokenHash(hash(rawToken.trim()))
                .orElseThrow(() -> BusinessException.badRequest(
                        "RESET_TOKEN_INVALID",
                        "Password reset token is invalid"
                ));
        if (token.getUsedAt() != null || !token.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw BusinessException.badRequest(
                    "RESET_TOKEN_EXPIRED",
                    "Password reset token has expired or was already used"
            );
        }
        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepo.save(user);
        token.setUsedAt(LocalDateTime.now());
        tokenRepo.save(token);
        refreshTokenRepo.revokeAllActiveTokensByUserId(user.getId(), LocalDateTime.now());
    }

    private void sendResetEmail(User user, String rawToken) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(user.getEmail());
        message.setSubject("Đặt lại mật khẩu Pet Shop");
        message.setText("""
                Xin chào %s,

                Mở liên kết sau để đặt lại mật khẩu (liên kết hết hạn sau %d phút):
                %s?token=%s

                Nếu bạn không yêu cầu thao tác này, hãy bỏ qua email.
                """.formatted(user.getName(), expirationMinutes, resetUrl, rawToken));
        mailSender.send(message);
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(rawToken.getBytes(StandardCharsets.UTF_8))
            );
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }
}
