package com.fiapx.api.service;

import com.fiapx.api.application.dto.AuthRequest;
import com.fiapx.api.application.dto.AuthResponse;
import com.fiapx.api.application.dto.RegisterRequest;
import com.fiapx.api.application.service.AuthService;
import com.fiapx.api.domain.entity.User;
import com.fiapx.api.domain.entity.enums.UserRole;
import com.fiapx.api.domain.repository.UserRepository;
import com.fiapx.api.infrastructure.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .name("Michel Oliveira")
                .email("michel@fiap.com.br")
                .password("encoded_pass")
                .role(UserRole.USER)
                .build();
    }

    @Test
    @DisplayName("Deve registrar novo usuário com sucesso")
    void shouldRegisterUserSuccessfully() {
        RegisterRequest request = RegisterRequest.builder()
                .name("Michel Oliveira")
                .email("michel@fiap.com.br")
                .password("123456")
                .build();

        when(userRepository.existsByEmail("michel@fiap.com.br")).thenReturn(false);
        when(passwordEncoder.encode("123456")).thenReturn("encoded_pass");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(jwtService.generateToken(any(User.class))).thenReturn("mock_token");
        when(jwtService.getExpirationTime()).thenReturn(86400000L);

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("mock_token", response.getToken());
        assertEquals("michel@fiap.com.br", response.getEmail());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar registrar com e-mail duplicado")
    void shouldThrowExceptionWhenRegisteringDuplicateEmail() {
        RegisterRequest request = RegisterRequest.builder()
                .name("Michel Oliveira")
                .email("michel@fiap.com.br")
                .password("123456")
                .build();

        when(userRepository.existsByEmail("michel@fiap.com.br")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Deve autenticar usuário com sucesso")
    void shouldAuthenticateUserSuccessfully() {
        AuthRequest request = AuthRequest.builder()
                .email("michel@fiap.com.br")
                .password("123456")
                .build();

        when(userRepository.findByEmail("michel@fiap.com.br")).thenReturn(Optional.of(sampleUser));
        when(jwtService.generateToken(sampleUser)).thenReturn("mock_token");
        when(jwtService.getExpirationTime()).thenReturn(86400000L);

        AuthResponse response = authService.authenticate(request);

        assertNotNull(response);
        assertEquals("mock_token", response.getToken());
        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao falhar autenticação com credenciais inválidas")
    void shouldThrowExceptionWhenBadCredentials() {
        AuthRequest request = AuthRequest.builder()
                .email("michel@fiap.com.br")
                .password("wrong_password")
                .build();

        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));

        assertThrows(BadCredentialsException.class, () -> authService.authenticate(request));
    }
}
