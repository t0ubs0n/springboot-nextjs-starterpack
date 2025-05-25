package com.toubson.modulith.user.dto;

import com.toubson.modulith.user.domain.UserRole;

import java.util.Set;

public record UserResponse(
        String id,
        String username,
        String email,
        String firstName,
        String lastName,
        boolean emailVerified,
        boolean enabled,
        Set<UserRole> roles,
        String createdAt,
        String updatedAt) {
}
