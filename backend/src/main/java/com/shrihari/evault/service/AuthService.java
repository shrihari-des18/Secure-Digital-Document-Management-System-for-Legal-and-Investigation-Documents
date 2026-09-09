package com.shrihari.evault.service;

import com.shrihari.evault.common.ActorContext;
import com.shrihari.evault.common.ActorContextHolder;
import com.shrihari.evault.dto.AuthResponse;
import com.shrihari.evault.dto.LoginRequest;
import com.shrihari.evault.dto.RegisterRequest;
import com.shrihari.evault.dto.UserResponse;
import com.shrihari.evault.exception.DuplicateResourceException;
import com.shrihari.evault.exception.ResourceNotFoundException;
import com.shrihari.evault.model.User;
import com.shrihari.evault.repository.UserRepository;
import com.shrihari.evault.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("Attempting authentication for user: {}", request.getUsername());

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.getUsername()));

        String token = jwtTokenProvider.generateToken(user);

        log.info("User {} successfully authenticated", user.getUsername());
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresInMs(jwtTokenProvider.getExpirationMs())
                .user(UserResponse.fromEntity(user))
                .build();
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user: {}", request.getUsername());

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username '" + request.getUsername() + "' is already taken");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email '" + request.getEmail() + "' is already registered");
        }

        User user = User.builder()
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .email(request.getEmail())
                .badgeNumber(request.getBadgeNumber())
                .department(request.getDepartment())
                .role(request.getRole())
                .active(true)
                .build();

        User saved = userRepository.save(user);
        String token = jwtTokenProvider.generateToken(saved);

        log.info("User {} registered successfully with ID: {}", saved.getUsername(), saved.getId());
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresInMs(jwtTokenProvider.getExpirationMs())
                .user(UserResponse.fromEntity(saved))
                .build();
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        ActorContext actor = ActorContextHolder.get();
        if (actor == null || actor.getUsername() == null) {
            throw new ResourceNotFoundException("No authenticated user in context");
        }

        User user = userRepository.findByUsername(actor.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + actor.getUsername()));

        return UserResponse.fromEntity(user);
    }
}
