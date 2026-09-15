package com.fiapx.api.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiapx.api.application.dto.AuthRequest;
import com.fiapx.api.application.dto.AuthResponse;
import com.fiapx.api.application.dto.RegisterRequest;
import com.fiapx.api.application.service.AuthService;
import com.fiapx.api.infrastructure.security.JwtAuthenticationFilter;
import com.fiapx.api.infrastructure.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @DisplayName("Deve registrar usuário via POST /auth/register")
    void shouldRegisterUserEndpoint() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .name("Michel")
                .email("michel@fiap.com.br")
                .password("123456")
                .build();

        AuthResponse response = AuthResponse.builder()
                .token("mock_jwt")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .email("michel@fiap.com.br")
                .name("Michel")
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("mock_jwt"))
                .andExpect(jsonPath("$.email").value("michel@fiap.com.br"));
    }

    @Test
    @DisplayName("Deve autenticar usuário via POST /auth/login")
    void shouldLoginUserEndpoint() throws Exception {
        AuthRequest request = AuthRequest.builder()
                .email("michel@fiap.com.br")
                .password("123456")
                .build();

        AuthResponse response = AuthResponse.builder()
                .token("mock_jwt")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .email("michel@fiap.com.br")
                .name("Michel")
                .build();

        when(authService.authenticate(any(AuthRequest.class))).thenReturn(response);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mock_jwt"));
    }
}
