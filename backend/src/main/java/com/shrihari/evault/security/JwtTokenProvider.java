package com.shrihari.evault.security;

import com.shrihari.evault.model.RoleEnum;
import com.shrihari.evault.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
@Slf4j
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtTokenProvider(
            @Value("${evault.jwt.secret:SIH26190SecureDigitalDocumentVaultSecretKeyForJwtAuthenticationToken2026}") String secret,
            @Value("${evault.jwt.expiration-ms:86400000}") long expirationMs) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(user.getUsername())
                .claim("userId", user.getId().toString())
                .claim("role", user.getRole().name())
                .claim("fullName", user.getFullName())
                .claim("email", user.getEmail())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String getUsername(String token) {
        return getClaims(token).getSubject();
    }

    public UUID getUserId(String token) {
        String userIdStr = getClaims(token).get("userId", String.class);
        return UUID.fromString(userIdStr);
    }

    public RoleEnum getRole(String token) {
        String roleStr = getClaims(token).get("role", String.class);
        return RoleEnum.valueOf(roleStr);
    }

    public long getExpirationMs() {
        return expirationMs;
    }
}
