package com.shrihari.evault.service;

import com.shrihari.evault.dto.CreateUserRequest;
import com.shrihari.evault.dto.UserResponse;
import com.shrihari.evault.exception.DuplicateResourceException;
import com.shrihari.evault.exception.ResourceNotFoundException;
import com.shrihari.evault.model.RoleEnum;
import com.shrihari.evault.model.User;
import com.shrihari.evault.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private CreateUserRequest request;

    @BeforeEach
    void setUp() {
        request = CreateUserRequest.builder()
                .username("analyst_priya")
                .fullName("Dr. Priya Verma")
                .email("priya.verma@cfs.gov.in")
                .badgeNumber("FSL-402")
                .department("Digital Forensics Lab")
                .role(RoleEnum.FORENSIC_ANALYST)
                .build();
    }

    @Test
    @DisplayName("Create user successfully when unique credentials are provided")
    void shouldCreateUser() {
        when(userRepository.existsByUsername("analyst_priya")).thenReturn(false);
        when(userRepository.existsByEmail("priya.verma@cfs.gov.in")).thenReturn(false);
        when(passwordEncoder.encode(any(CharSequence.class))).thenReturn("hashedPassword");

        User savedUser = User.builder()
                .username(request.getUsername())
                .fullName(request.getFullName())
                .email(request.getEmail())
                .badgeNumber(request.getBadgeNumber())
                .department(request.getDepartment())
                .role(request.getRole())
                .active(true)
                .build();
        savedUser.setId(UUID.randomUUID());

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserResponse response = userService.createUser(request);

        assertThat(response).isNotNull();
        assertThat(response.getUsername()).isEqualTo("analyst_priya");
        assertThat(response.getRole()).isEqualTo(RoleEnum.FORENSIC_ANALYST);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Throw DuplicateResourceException when username already exists")
    void shouldThrowWhenUsernameExists() {
        when(userRepository.existsByUsername("analyst_priya")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already taken");
    }

    @Test
    @DisplayName("Throw ResourceNotFoundException when user ID does not exist")
    void shouldThrowWhenUserNotFound() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }
}
