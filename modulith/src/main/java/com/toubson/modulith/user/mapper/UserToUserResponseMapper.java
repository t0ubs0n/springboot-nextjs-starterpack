package com.toubson.modulith.user.mapper;

import com.toubson.modulith.user.domain.User;
import com.toubson.modulith.user.dto.UserResponse;

public class UserToUserResponseMapper {

    public static UserResponse mapToResponse(User user) {
        return new UserResponse(
                user.getId().toString(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.isEmailVerified(),
                user.isEnabled(),
                user.getRoles(),
                user.getCreatedAt().toString(),
                user.getUpdatedAt() != null ? user.getUpdatedAt().toString() : null
        );
    }
}
