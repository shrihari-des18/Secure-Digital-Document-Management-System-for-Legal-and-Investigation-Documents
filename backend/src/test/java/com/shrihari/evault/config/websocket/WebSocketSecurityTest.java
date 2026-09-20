package com.shrihari.evault.config.websocket;

import com.shrihari.evault.model.RoleEnum;
import com.shrihari.evault.model.User;
import com.shrihari.evault.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebSocketSecurityTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private com.shrihari.evault.repository.CaseRepository caseRepository;

    @InjectMocks
    private JwtChannelInterceptor jwtChannelInterceptor;

    private MessageChannel channel;

    @BeforeEach
    void setUp() {
        channel = mock(MessageChannel.class);
        ReflectionTestUtils.setField(jwtChannelInterceptor, "allowAnonymousFallback", false);
    }

    @Test
    @DisplayName("CONNECT frame with valid JWT authenticates principal with granted authorities")
    void testConnectWithValidToken() {
        String token = "valid.jwt.token";
        UUID userId = UUID.randomUUID();

        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getUsername(token)).thenReturn("admin_user");
        when(jwtTokenProvider.getRole(token)).thenReturn(RoleEnum.SYSTEM_ADMIN);
        when(jwtTokenProvider.getUserId(token)).thenReturn(userId);

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setLeaveMutable(true);
        accessor.addNativeHeader("Authorization", "Bearer " + token);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = jwtChannelInterceptor.preSend(message, channel);

        assertThat(result).isNotNull();
        StompHeaderAccessor postAccessor = StompHeaderAccessor.wrap(result);
        assertThat(postAccessor.getUser()).isNotNull();
        assertThat(postAccessor.getUser().getName()).isEqualTo("admin_user");
        Authentication auth = (Authentication) postAccessor.getUser();
        assertThat(auth.getAuthorities()).extracting("authority").contains("ROLE_SYSTEM_ADMIN");
    }

    @Test
    @DisplayName("CONNECT frame without token throws IllegalArgumentException when anonymous fallback is disabled")
    void testConnectWithoutTokenFails() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatThrownBy(() -> jwtChannelInterceptor.preSend(message, channel))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Authentication required");
    }

    @Test
    @DisplayName("SUBSCRIBE to /topic/security/alarms allowed for SYSTEM_ADMIN and AUDITOR")
    void testSubscribeSecurityAlarmsAllowed() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/security/alarms");
        accessor.setUser(new UsernamePasswordAuthenticationToken(
                "auditor_user", null, List.of(new SimpleGrantedAuthority("ROLE_AUDITOR"))));

        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        Message<?> result = jwtChannelInterceptor.preSend(message, channel);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("SUBSCRIBE to /topic/security/alarms rejected for INVESTIGATING_OFFICER with AccessDeniedException")
    void testSubscribeSecurityAlarmsDenied() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/security/alarms");
        accessor.setUser(new UsernamePasswordAuthenticationToken(
                "officer_user", null, List.of(new SimpleGrantedAuthority("ROLE_INVESTIGATING_OFFICER"))));

        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatThrownBy(() -> jwtChannelInterceptor.preSend(message, channel))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("SYSTEM_ADMIN or AUDITOR role");
    }

    @Test
    @DisplayName("CONNECT with anonymous fallback enabled assigns unprivileged ROLE_ANONYMOUS")
    void testConnectWithAnonymousFallback() {
        ReflectionTestUtils.setField(jwtChannelInterceptor, "allowAnonymousFallback", true);

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = jwtChannelInterceptor.preSend(message, channel);

        assertThat(result).isNotNull();
        StompHeaderAccessor postAccessor = StompHeaderAccessor.wrap(result);
        Authentication auth = (Authentication) postAccessor.getUser();
        assertThat(auth.getAuthorities()).extracting("authority").contains("ROLE_ANONYMOUS");
        assertThat(auth.getAuthorities()).extracting("authority").doesNotContain("ROLE_SYSTEM_ADMIN");
    }

    @Test
    @DisplayName("SUBSCRIBE to /topic/case/{caseId} allowed for assigned officer")
    void testSubscribeCaseTopicAllowedForAssignedOfficer() {
        UUID caseId = UUID.randomUUID();
        UUID officerId = UUID.randomUUID();

        com.shrihari.evault.model.Case c = com.shrihari.evault.model.Case.builder()
                .leadOfficerId(officerId)
                .build();
        when(caseRepository.findById(caseId)).thenReturn(java.util.Optional.of(c));

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/case/" + caseId);
        accessor.setUser(new UsernamePasswordAuthenticationToken(
                "io_user", null, List.of(new SimpleGrantedAuthority("ROLE_INVESTIGATING_OFFICER"))));
        accessor.setSessionAttributes(new java.util.HashMap<>(Map.of("userId", officerId)));

        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        Message<?> result = jwtChannelInterceptor.preSend(message, channel);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("SUBSCRIBE to /topic/case/{caseId} rejected for unassigned officer")
    void testSubscribeCaseTopicDeniedForUnassignedOfficer() {
        UUID caseId = UUID.randomUUID();
        UUID officerId = UUID.randomUUID();
        UUID otherOfficerId = UUID.randomUUID();

        com.shrihari.evault.model.Case c = com.shrihari.evault.model.Case.builder()
                .leadOfficerId(otherOfficerId)
                .build();
        when(caseRepository.findById(caseId)).thenReturn(java.util.Optional.of(c));

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/case/" + caseId);
        accessor.setUser(new UsernamePasswordAuthenticationToken(
                "io_user", null, List.of(new SimpleGrantedAuthority("ROLE_INVESTIGATING_OFFICER"))));
        accessor.setSessionAttributes(new java.util.HashMap<>(Map.of("userId", officerId)));

        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatThrownBy(() -> jwtChannelInterceptor.preSend(message, channel))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("not assigned to case");
    }
}
