package com.shrihari.evault.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Column(name = "username", nullable = false, unique = true, length = 64)
    private String username;

    @Column(name = "full_name", nullable = false, length = 128)
    private String fullName;

    @Column(name = "email", nullable = false, unique = true, length = 128)
    private String email;

    @Column(name = "badge_number", length = 64)
    private String badgeNumber;

    @Column(name = "department", length = 128)
    private String department;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 32)
    private RoleEnum role;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private boolean active = true;
}
