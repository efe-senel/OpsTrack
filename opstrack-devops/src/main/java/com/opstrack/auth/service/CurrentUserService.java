package com.opstrack.auth.service;

import com.opstrack.auth.entity.User;
import com.opstrack.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public User require(Authentication authentication) {
        String normalizedEmail = authentication.getName()
                .trim()
                .toLowerCase(Locale.ROOT);

        return userRepository
                .findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Authenticated user not found"));
    }
}
