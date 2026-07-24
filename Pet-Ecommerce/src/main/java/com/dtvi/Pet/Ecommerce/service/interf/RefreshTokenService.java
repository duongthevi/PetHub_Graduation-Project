package com.dtvi.Pet.Ecommerce.service.interf;

import com.dtvi.Pet.Ecommerce.entity.RefreshToken;
import com.dtvi.Pet.Ecommerce.entity.User;

public interface RefreshTokenService {

    IssuedRefreshToken issue(User user, String deviceInfo);

    IssuedRefreshToken rotate(String rawRefreshToken, String deviceInfo);

    void revoke(String rawRefreshToken);

    void revokeAll(String rawRefreshToken);

    record IssuedRefreshToken(String rawToken, User user, RefreshToken tokenEntity) {
    }
}
