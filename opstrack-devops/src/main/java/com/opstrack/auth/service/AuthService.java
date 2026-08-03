package com.opstrack.auth.service;

import com.opstrack.auth.dto.LoginRequest;
import com.opstrack.auth.dto.RegisterRequest;
import com.opstrack.auth.dto.UserResponse;
import com.opstrack.auth.entity.User;
import com.opstrack.auth.exception.EmailAlreadyExistsException;
import com.opstrack.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final CurrentUserService currentUserService;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String normalizedEmail = normalizeEmail(request.email());

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new EmailAlreadyExistsException(normalizedEmail);
        }

        String passwordHash = passwordEncoder.encode(request.password());

        User user = new User(
                request.name().trim(),
                normalizedEmail,
                passwordHash
        );

        return UserResponse.from(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public LoginResult login(LoginRequest request) {
        String normalizedEmail = normalizeEmail(request.email());

        UsernamePasswordAuthenticationToken authenticationRequest =
                UsernamePasswordAuthenticationToken.unauthenticated(
                        normalizedEmail,
                        request.password()
                );

        Authentication authentication =
                authenticationManager.authenticate(authenticationRequest);

        User user = userRepository
                .findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() ->
                        new UsernameNotFoundException("Invalid email or password")
                );

        return new LoginResult(
                authentication,
                UserResponse.from(user)
        );
    }

    @Transactional(readOnly = true)
    public UserResponse currentUser(Authentication authentication) {
        return UserResponse.from(currentUserService.require(authentication));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
