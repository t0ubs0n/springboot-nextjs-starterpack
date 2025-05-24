package com.toubson.modulith.user.dto;

import com.toubson.modulith.user.domain.UserRole;

import java.util.Set;

public record UserResponse(
        String id,
        String username,
        String email,
        Set<UserRole> roles,
        String createdAt) {
}
