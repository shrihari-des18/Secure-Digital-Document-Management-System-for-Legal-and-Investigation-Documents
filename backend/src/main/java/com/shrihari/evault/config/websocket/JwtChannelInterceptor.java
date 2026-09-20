package com.shrihari.evault.config.websocket;

import com.shrihari.evault.model.RoleEnum;
import com.shrihari.evault.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final com.shrihari.evault.repository.CaseRepository caseRepository;

    @Value("${evault.security.allow-anonymous-fallback:false}")
    private boolean allowAnonymousFallback;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            authenticateConnect(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            authorizeSubscription(accessor);
        }

        return message;
    }

    private void authenticateConnect(StompHeaderAccessor accessor) {
        String token = extractToken(accessor);

        if (token != null && jwtTokenProvider.validateToken(token)) {
            String username = jwtTokenProvider.getUsername(token);
            RoleEnum role = jwtTokenProvider.getRole(token);
            UUID userId = jwtTokenProvider.getUserId(token);

            List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(username, null, authorities);
            accessor.setUser(authentication);
            if (accessor.getSessionAttributes() != null) {
                accessor.getSessionAttributes().put("userId", userId);
                accessor.getSessionAttributes().put("role", role);
            }
            log.info("WebSocket STOMP authenticated: user={}, role={}, userId={}", username, role, userId);
        } else if (allowAnonymousFallback) {
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken("anonymous", null,
                            List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
            accessor.setUser(authentication);
            log.debug("WebSocket STOMP connected with unprivileged anonymous fallback");
        } else {
            log.warn("WebSocket STOMP CONNECT rejected: missing or invalid authentication token");
            throw new IllegalArgumentException("Authentication required for WebSocket connection");
        }
    }

    private void authorizeSubscription(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null) {
            return;
        }

        Principal principal = accessor.getUser();
        if (principal == null) {
            throw new AccessDeniedException("Authentication required for STOMP subscription");
        }

        if (destination.startsWith("/topic/security/alarms")) {
            boolean isAuthorized = false;

            if (principal instanceof Authentication auth) {
                isAuthorized = auth.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_SYSTEM_ADMIN")
                                || a.getAuthority().equals("ROLE_AUDITOR"));
            }

            if (!isAuthorized) {
                log.warn("WebSocket subscription to {} denied for principal {}", destination, principal.getName());
                throw new AccessDeniedException("Access denied: Subscribing to security alarms requires SYSTEM_ADMIN or AUDITOR role");
            }
        } else if (destination.startsWith("/topic/case/")) {
            if (principal instanceof Authentication auth) {
                boolean isPrivileged = auth.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_SYSTEM_ADMIN")
                                || a.getAuthority().equals("ROLE_AUDITOR")
                                || a.getAuthority().equals("ROLE_COURT")
                                || a.getAuthority().equals("ROLE_PROSECUTOR")
                                || a.getAuthority().equals("ROLE_FORENSIC_ANALYST"));

                if (!isPrivileged && caseRepository != null) {
                    try {
                        String caseIdStr = destination.substring("/topic/case/".length());
                        UUID caseId = UUID.fromString(caseIdStr);
                        Object userIdObj = accessor.getSessionAttributes() != null
                                ? accessor.getSessionAttributes().get("userId")
                                : null;

                        if (userIdObj instanceof UUID userId) {
                            var caseOpt = caseRepository.findById(caseId);
                            if (caseOpt.isPresent()) {
                                var c = caseOpt.get();
                                boolean isAssigned = userId.equals(c.getLeadOfficerId())
                                        || (c.getAssignedOfficerIds() != null && c.getAssignedOfficerIds().contains(userId));
                                if (!isAssigned) {
                                    log.warn("WebSocket subscription to case {} denied for officer {}", caseId, userId);
                                    throw new AccessDeniedException("Access denied: You are not assigned to case " + caseId);
                                }
                            }
                        }
                    } catch (IllegalArgumentException ignored) {
                        // Non-UUID sub-paths pass or ignored
                    }
                }
            }
        }
    }

    private String extractToken(StompHeaderAccessor accessor) {
        String token = null;

        // 1. Try native headers
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        } else if (authHeader != null && !authHeader.isBlank()) {
            token = authHeader;
        }

        if (token == null || token.isBlank()) {
            token = accessor.getFirstNativeHeader("token");
        }

        if (token == null || token.isBlank()) {
            token = accessor.getFirstNativeHeader("access_token");
        }

        // 2. Try session attributes from handshake
        if ((token == null || token.isBlank()) && accessor.getSessionAttributes() != null) {
            Object sessionToken = accessor.getSessionAttributes().get(JwtHandshakeInterceptor.TOKEN_ATTR);
            if (sessionToken instanceof String strToken) {
                token = strToken;
            }
        }

        return token;
    }
}
