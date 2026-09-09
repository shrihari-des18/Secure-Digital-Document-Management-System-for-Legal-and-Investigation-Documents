package com.shrihari.evault.security;

import com.shrihari.evault.common.ActorContext;
import com.shrihari.evault.common.ActorContextHolder;
import com.shrihari.evault.model.RoleEnum;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    public static final String HEADER_ACTOR_ID = "X-Actor-Id";
    public static final String HEADER_ACTOR_ROLE = "X-Actor-Role";
    public static final String HEADER_ACTOR_NAME = "X-Actor-Name";

    @org.springframework.beans.factory.annotation.Value("${evault.security.allow-anonymous-fallback:false}")
    private boolean allowAnonymousFallback;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = extractJwtFromRequest(request);

            if (StringUtils.hasText(token)) {
                if (jwtTokenProvider.validateToken(token)) {
                    String username = jwtTokenProvider.getUsername(token);
                    UUID userId = jwtTokenProvider.getUserId(token);
                    RoleEnum role = jwtTokenProvider.getRole(token);

                    List<SimpleGrantedAuthority> authorities = List.of(
                            new SimpleGrantedAuthority("ROLE_" + role.name())
                    );

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(username, null, authorities);
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    ActorContextHolder.set(ActorContext.builder()
                            .actorId(userId)
                            .username(username)
                            .role(role)
                            .build());
                }
            } else if (hasFallbackHeaders(request)) {
                // Backward-compatible fallback for testing / dev
                setupFallbackAuthentication(request);
            } else if (allowAnonymousFallback) {
                setupDefaultOfficerAuthentication(request);
            }

            filterChain.doFilter(request, response);
        } finally {
            ActorContextHolder.clear();
        }
    }

    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private boolean hasFallbackHeaders(HttpServletRequest request) {
        return StringUtils.hasText(request.getHeader(HEADER_ACTOR_ID)) ||
               StringUtils.hasText(request.getHeader(HEADER_ACTOR_ROLE));
    }

    private void setupFallbackAuthentication(HttpServletRequest request) {
        String actorIdStr = request.getHeader(HEADER_ACTOR_ID);
        String roleStr = request.getHeader(HEADER_ACTOR_ROLE);
        String actorName = request.getHeader(HEADER_ACTOR_NAME);

        UUID actorId = null;
        if (StringUtils.hasText(actorIdStr)) {
            try {
                actorId = UUID.fromString(actorIdStr.trim());
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (actorId == null) {
            actorId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        }

        RoleEnum role = RoleEnum.INVESTIGATING_OFFICER;
        if (StringUtils.hasText(roleStr)) {
            try {
                role = RoleEnum.valueOf(roleStr.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }

        String username = StringUtils.hasText(actorName) ? actorName.trim() : "OFFICER_DEFAULT";

        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + role.name())
        );

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(username, null, authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        ActorContextHolder.set(ActorContext.builder()
                .actorId(actorId)
                .username(username)
                .role(role)
                .build());
    }

    private void setupDefaultOfficerAuthentication(HttpServletRequest request) {
        UUID actorId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        RoleEnum role = RoleEnum.INVESTIGATING_OFFICER;
        String username = "OFFICER_DEFAULT";

        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + role.name())
        );

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(username, null, authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        ActorContextHolder.set(ActorContext.builder()
                .actorId(actorId)
                .username(username)
                .role(role)
                .build());
    }
}
