package com.fiapx.api.security;

import com.fiapx.api.domain.entity.User;
import com.fiapx.api.domain.entity.enums.UserRole;
import com.fiapx.api.infrastructure.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 3600000L);

        user = User.builder()
                .id(1L)
                .email("michel@fiap.com.br")
                .name("Michel Oliveira")
                .role(UserRole.USER)
                .build();
    }

    @Test
    @DisplayName("Deve gerar token JWT válido e extrair username corretamente")
    void shouldGenerateAndValidateToken() {
        String token = jwtService.generateToken(user);

        assertNotNull(token);
        assertFalse(token.isEmpty());

        String username = jwtService.extractUsername(token);
        assertEquals("michel@fiap.com.br", username);

        assertTrue(jwtService.isTokenValid(token, user));
    }

    @Test
    @DisplayName("Deve gerar token com claims extras")
    void shouldGenerateTokenWithExtraClaims() {
        String token = jwtService.generateToken(java.util.Map.of("role", "USER"), user);

        assertNotNull(token);
        assertEquals("michel@fiap.com.br", jwtService.extractUsername(token));
    }

    @Test
    @DisplayName("Deve retornar false ao validar token para um usuário diferente")
    void shouldReturnFalseForMismatchedUser() {
        String token = jwtService.generateToken(user);

        User outroUsuario = User.builder()
                .id(2L)
                .email("outro@fiap.com.br")
                .name("Outro")
                .role(UserRole.USER)
                .build();

        assertFalse(jwtService.isTokenValid(token, outroUsuario));
    }

    @Test
    @DisplayName("Deve lançar ExpiredJwtException ao validar um token expirado")
    void shouldThrowForExpiredToken() {
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", -10000L);
        String expiredToken = jwtService.generateToken(user);

        assertThrows(io.jsonwebtoken.ExpiredJwtException.class,
                () -> jwtService.isTokenValid(expiredToken, user));
    }

    @Test
    @DisplayName("Deve retornar o tempo de expiração configurado")
    void shouldReturnConfiguredExpirationTime() {
        assertEquals(3600000L, jwtService.getExpirationTime());
    }
}
