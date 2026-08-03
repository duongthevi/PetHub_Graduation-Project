package com.dtvi.Pet.Ecommerce.service.impl;

import com.dtvi.Pet.Ecommerce.entity.User;
import com.dtvi.Pet.Ecommerce.security.AuthUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentActorService {

    public User getOrNull() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null
                && authentication.getPrincipal()
                instanceof AuthUser authUser) {
            return authUser.getUser();
        }

        return null;
    }

    public Long getIdOrNull() {
        User user = getOrNull();
        return user == null ? null : user.getId();
    }
}