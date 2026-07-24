package com.dtvi.Pet.Ecommerce.service.interf;

import com.dtvi.Pet.Ecommerce.dto.AuthResponse;
import com.dtvi.Pet.Ecommerce.dto.LoginRequest;

public interface AuthService {

    AuthSession login(LoginRequest loginRequest, String deviceInfo);

    AuthSession refresh(String rawRefreshToken, String deviceInfo);

    void logout(String rawRefreshToken);

    void logoutAll(String rawRefreshToken);

    record AuthSession(AuthResponse response, String refreshToken) {
    }
}
