package com.shrihari.evault.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shrihari.evault.dto.LoginRequest;
import com.shrihari.evault.dto.RegisterRequest;
import com.shrihari.evault.model.RoleEnum;
import com.shrihari.evault.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Complete Auth flow: Register, Login, Extract Profile with Bearer Token, and Reject Bad Credentials")
    void testAuthLifecycle() throws Exception {
        // 1. Register a new user
        RegisterRequest registerRequest = RegisterRequest.builder()
                .username("officer_rohit")
                .password("StrongPassword@2026")
                .fullName("Inspector Rohit Varma")
                .email("rohit.varma@police.gov.in")
                .badgeNumber("MH-4019")
                .department("Special Investigation Cell")
                .role(RoleEnum.INVESTIGATING_OFFICER)
                .build();

        String registerResponseJson = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.token", notNullValue()))
                .andExpect(jsonPath("$.data.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.data.user.username", is("officer_rohit")))
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(registerResponseJson).path("data").path("token").asText();

        // 2. Access /api/v1/auth/me with Bearer token
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.username", is("officer_rohit")))
                .andExpect(jsonPath("$.data.role", is("INVESTIGATING_OFFICER")));

        // 3. Login with correct credentials
        LoginRequest loginRequest = LoginRequest.builder()
                .username("officer_rohit")
                .password("StrongPassword@2026")
                .build();

        String loginResponseJson = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.token", notNullValue()))
                .andReturn().getResponse().getContentAsString();

        String loginToken = objectMapper.readTree(loginResponseJson).path("data").path("token").asText();

        // 4. Validate login token works for authenticated profile
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + loginToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email", is("rohit.varma@police.gov.in")));

        // 5. Login with invalid password fails
        LoginRequest badLoginRequest = LoginRequest.builder()
                .username("officer_rohit")
                .password("WrongPassword123")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badLoginRequest)))
                .andExpect(status().isUnauthorized());

        // 6. Duplicate registration fails
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Reject request with invalid or tampered JWT Bearer token")
    void testTamperedTokenRejected() throws Exception {
        String forgedToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiJ9.invalidsignaturehere";

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + forgedToken))
                .andExpect(status().isUnauthorized());
    }
}
