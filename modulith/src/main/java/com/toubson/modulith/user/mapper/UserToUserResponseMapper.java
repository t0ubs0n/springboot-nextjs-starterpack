package com.toubson.modulith.user.mapper;

import com.toubson.modulith.user.domain.User;
import com.toubson.modulith.user.dto.UserResponse;

public class UserToUserResponseMapper {

    public static UserResponse mapToResponse(User user) {
        return new UserResponse(
                user.getId().toString(),
                user.getUsername(),
                user.getEmail(),
                user.getRoles(),
                user.getCreatedAt().toString()
        );
    }
}
