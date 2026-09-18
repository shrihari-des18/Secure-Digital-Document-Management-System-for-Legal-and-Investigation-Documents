package com.shrihari.evault.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shrihari.evault.model.RoleEnum;
import com.shrihari.evault.model.User;
import com.shrihari.evault.repository.UserRepository;
import com.shrihari.evault.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SearchControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String jwtToken;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        User officer = User.builder()
                .username("officer_search_test")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .fullName("Inspector Search")
                .email("search@police.gov.in")
                .badgeNumber("DL-999")
                .department("Cyber")
                .role(RoleEnum.INVESTIGATING_OFFICER)
                .build();

        User savedUser = userRepository.save(officer);
        jwtToken = jwtTokenProvider.generateToken(savedUser);
    }

    @Test
    void testSearchEndpointWithValidToken() throws Exception {
        mockMvc.perform(get("/api/v1/search")
                        .header("Authorization", "Bearer " + jwtToken)
                        .param("query", "cyber")
                        .param("type", "ALL")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.hits").isArray());
    }
}
