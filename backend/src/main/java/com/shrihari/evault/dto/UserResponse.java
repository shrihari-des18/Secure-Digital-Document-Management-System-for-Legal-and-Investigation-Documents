package com.shrihari.evault.dto;

import com.shrihari.evault.model.RoleEnum;
import com.shrihari.evault.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private UUID id;
    private String username;
    private String fullName;
    private String email;
    private String badgeNumber;
    private String department;
    private RoleEnum role;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;

    public static UserResponse fromEntity(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .badgeNumber(user.getBadgeNumber())
                .department(user.getDepartment())
                .role(user.getRole())
                .active(user.isActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
