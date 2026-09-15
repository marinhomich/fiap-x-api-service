package com.fiapx.api.infrastructure.security;

import com.fiapx.api.domain.entity.User;
import com.fiapx.api.domain.entity.enums.UserRole;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {

    private JwtService jwtService;
    private UserDetailsService userDetailsService;
    private JwtAuthenticationFilter filter;

    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        jwtService = mock(JwtService.class);
        userDetailsService = mock(UserDetailsService.class);
        filter = new JwtAuthenticationFilter(jwtService, userDetailsService);

        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        filterChain = mock(FilterChain.class);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Deve seguir a cadeia sem autenticar quando não há header Authorization")
    void shouldSkipWhenNoAuthorizationHeader() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("Deve seguir a cadeia sem autenticar quando o header não é Bearer")
    void shouldSkipWhenHeaderIsNotBearer() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic abc123");

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("Deve autenticar o usuário quando o token é válido")
    void shouldAuthenticateWhenTokenIsValid() throws Exception {
        User user = User.builder().id(1L).email("michel@fiap.com.br").role(UserRole.USER).build();

        when(request.getHeader("Authorization")).thenReturn("Bearer valid.token.here");
        when(jwtService.extractUsername("valid.token.here")).thenReturn("michel@fiap.com.br");
        when(userDetailsService.loadUserByUsername("michel@fiap.com.br")).thenReturn(user);
        when(jwtService.isTokenValid("valid.token.here", user)).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(user, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Não deve autenticar quando o token é inválido")
    void shouldNotAuthenticateWhenTokenIsInvalid() throws Exception {
        User user = User.builder().id(1L).email("michel@fiap.com.br").role(UserRole.USER).build();

        when(request.getHeader("Authorization")).thenReturn("Bearer invalid.token");
        when(jwtService.extractUsername("invalid.token")).thenReturn("michel@fiap.com.br");
        when(userDetailsService.loadUserByUsername("michel@fiap.com.br")).thenReturn(user);
        when(jwtService.isTokenValid("invalid.token", user)).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Deve seguir a cadeia sem lançar exceção quando o token está expirado/corrompido")
    void shouldContinueChainWhenTokenParsingFails() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer broken.token");
        when(jwtService.extractUsername("broken.token")).thenThrow(new RuntimeException("token expirado"));

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(userDetailsService);
    }

    @Test
    @DisplayName("Não deve reautenticar quando já existe autenticação no contexto")
    void shouldSkipWhenAlreadyAuthenticated() throws Exception {
        User user = User.builder().id(1L).email("michel@fiap.com.br").role(UserRole.USER).build();
        org.springframework.security.authentication.UsernamePasswordAuthenticationToken existingAuth =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(existingAuth);

        when(request.getHeader("Authorization")).thenReturn("Bearer some.token");
        when(jwtService.extractUsername("some.token")).thenReturn("michel@fiap.com.br");

        filter.doFilterInternal(request, response, filterChain);

        assertSame(existingAuth, SecurityContextHolder.getContext().getAuthentication());
        verify(userDetailsService, never()).loadUserByUsername(any());
        verify(filterChain).doFilter(request, response);
    }
}
