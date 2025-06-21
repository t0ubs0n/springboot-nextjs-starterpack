package com.toubson.modulith.identity.dto;

import com.toubson.modulith.identity.domain.UserRole;

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
