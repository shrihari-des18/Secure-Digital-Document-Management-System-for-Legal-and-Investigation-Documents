package com.shrihari.evault.common;

import com.shrihari.evault.model.RoleEnum;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

@Component
public class ActorContextInterceptor implements HandlerInterceptor {

    public static final String HEADER_ACTOR_ID = "X-Actor-Id";
    public static final String HEADER_ACTOR_ROLE = "X-Actor-Role";
    public static final String HEADER_ACTOR_NAME = "X-Actor-Name";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (ActorContextHolder.get() != null) {
            return true;
        }

        String actorIdStr = request.getHeader(HEADER_ACTOR_ID);
        String roleStr = request.getHeader(HEADER_ACTOR_ROLE);
        String actorName = request.getHeader(HEADER_ACTOR_NAME);

        UUID actorId = null;
        if (actorIdStr != null && !actorIdStr.isBlank()) {
            try {
                actorId = UUID.fromString(actorIdStr.trim());
            } catch (IllegalArgumentException ignored) {
                // Keep actorId null if invalid UUID
            }
        }

        RoleEnum role = RoleEnum.INVESTIGATING_OFFICER;
        if (roleStr != null && !roleStr.isBlank()) {
            try {
                role = RoleEnum.valueOf(roleStr.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // Keep default
            }
        }

        ActorContext context = ActorContext.builder()
                .actorId(actorId != null ? actorId : UUID.fromString("00000000-0000-0000-0000-000000000001"))
                .username(actorName != null && !actorName.isBlank() ? actorName.trim() : "OFFICER_DEFAULT")
                .role(role)
                .build();

        ActorContextHolder.set(context);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        ActorContextHolder.clear();
    }
}
