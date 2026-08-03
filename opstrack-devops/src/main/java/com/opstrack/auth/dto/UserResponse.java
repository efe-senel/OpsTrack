package com.opstrack.auth.dto;

import com.opstrack.auth.entity.User;
import com.opstrack.auth.entity.UserRole;

import java.time.OffsetDateTime;

public record UserResponse(
        Long id,
        String name,
        String email,
        UserRole role,
        OffsetDateTime createdAt
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}