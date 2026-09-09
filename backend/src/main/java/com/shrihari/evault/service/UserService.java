package com.shrihari.evault.service;

import com.shrihari.evault.dto.CreateUserRequest;
import com.shrihari.evault.dto.UserResponse;
import com.shrihari.evault.exception.DuplicateResourceException;
import com.shrihari.evault.exception.ResourceNotFoundException;
import com.shrihari.evault.model.User;
import com.shrihari.evault.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        log.info("Creating user with username: {}", request.getUsername());

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username '" + request.getUsername() + "' is already taken");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email '" + request.getEmail() + "' is already registered");
        }

        String rawPassword = StringUtils.hasText(request.getPassword()) ? request.getPassword() : "DefaultPass@123";

        User user = User.builder()
                .username(request.getUsername())
                .fullName(request.getFullName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(rawPassword))
                .badgeNumber(request.getBadgeNumber())
                .department(request.getDepartment())
                .role(request.getRole())
                .active(true)
                .build();

        User saved = userRepository.save(user);
        log.info("User created successfully with ID: {}", saved.getId());
        return UserResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID id) {
        User user = getEntityById(id);
        return UserResponse.fromEntity(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
        return UserResponse.fromEntity(user);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public User getEntityById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));
    }
}
