package com.dtvi.Pet.Ecommerce.service.interf;

import com.dtvi.Pet.Ecommerce.dto.Response;
import com.dtvi.Pet.Ecommerce.dto.UserDto;
import com.dtvi.Pet.Ecommerce.entity.User;
import org.springframework.data.domain.Page;

public interface UserService {
    Response registerUser(UserDto registrationRequest);
    Response getAllUsers();
    User getLoginUser();
    Response getUserInfoAndOrderHistory();

    Page<UserDto> searchUsers(
            String query,
            int page,
            int size
    );
}

