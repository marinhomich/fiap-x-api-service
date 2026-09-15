package com.fiapx.api.domain.entity;

import com.fiapx.api.domain.entity.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    @DisplayName("Deve expor o e-mail como username do Spring Security")
    void shouldExposeEmailAsUsername() {
        User user = User.builder().email("michel@fiap.com.br").role(UserRole.USER).build();

        assertEquals("michel@fiap.com.br", user.getUsername());
    }

    @Test
    @DisplayName("Deve expor a role como authority prefixada com ROLE_")
    void shouldExposeRoleAsAuthority() {
        User user = User.builder().email("michel@fiap.com.br").role(UserRole.ADMIN).build();

        Collection<? extends GrantedAuthority> authorities = user.getAuthorities();

        assertEquals(1, authorities.size());
        assertEquals("ROLE_ADMIN", authorities.iterator().next().getAuthority());
    }

    @Test
    @DisplayName("Conta deve estar sempre habilitada, não expirada e não bloqueada")
    void shouldAlwaysBeEnabledAndUnlocked() {
        User user = User.builder().email("michel@fiap.com.br").role(UserRole.USER).build();

        assertTrue(user.isAccountNonExpired());
        assertTrue(user.isAccountNonLocked());
        assertTrue(user.isCredentialsNonExpired());
        assertTrue(user.isEnabled());
    }

    @Test
    @DisplayName("onCreate deve definir createdAt e role padrão USER quando ausente")
    void shouldSetDefaultsOnCreate() {
        User user = new User();
        user.setEmail("novo@fiap.com.br");

        ReflectionTestUtils.invokeMethod(user, "onCreate");

        assertNotNull(user.getCreatedAt());
        assertEquals(UserRole.USER, user.getRole());
    }

    @Test
    @DisplayName("onCreate não deve sobrescrever a role já definida")
    void shouldNotOverrideExistingRoleOnCreate() {
        User user = new User();
        user.setEmail("admin@fiap.com.br");
        user.setRole(UserRole.ADMIN);

        ReflectionTestUtils.invokeMethod(user, "onCreate");

        assertEquals(UserRole.ADMIN, user.getRole());
    }
}
