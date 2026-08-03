package com.opstrack.auth.service;

import com.opstrack.auth.dto.UserResponse;
import org.springframework.security.core.Authentication;

public record LoginResult(
        Authentication authentication,
        UserResponse user
) {
}