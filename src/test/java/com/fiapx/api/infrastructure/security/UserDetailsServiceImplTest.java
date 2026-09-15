package com.fiapx.api.infrastructure.security;

import com.fiapx.api.domain.entity.User;
import com.fiapx.api.domain.entity.enums.UserRole;
import com.fiapx.api.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserDetailsServiceImplTest {

    private UserRepository userRepository;
    private UserDetailsServiceImpl service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        service = new UserDetailsServiceImpl(userRepository);
    }

    @Test
    @DisplayName("Deve carregar o usuário quando o e-mail existe")
    void shouldLoadUserWhenEmailExists() {
        User user = User.builder().id(1L).email("michel@fiap.com.br").role(UserRole.USER).build();
        when(userRepository.findByEmail("michel@fiap.com.br")).thenReturn(Optional.of(user));

        UserDetails result = service.loadUserByUsername("michel@fiap.com.br");

        assertEquals("michel@fiap.com.br", result.getUsername());
    }

    @Test
    @DisplayName("Deve lançar UsernameNotFoundException quando o e-mail não existe")
    void shouldThrowWhenEmailDoesNotExist() {
        when(userRepository.findByEmail("desconhecido@fiap.com.br")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> service.loadUserByUsername("desconhecido@fiap.com.br"));
    }
}
