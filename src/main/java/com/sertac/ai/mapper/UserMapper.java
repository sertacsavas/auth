package com.sertac.ai.mapper;

import com.sertac.ai.model.dto.UserResponse;
import com.sertac.ai.model.entity.User;

public class UserMapper {
    public static UserResponse mapUserToUserResponse(User user) {
        return new UserResponse(
            user.getId(),
            user.getEmail()
        );
    }
}
