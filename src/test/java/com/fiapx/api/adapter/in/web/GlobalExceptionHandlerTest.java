package com.fiapx.api.adapter.in.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.io.FileNotFoundException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("Deve tratar BadCredentialsException com 401")
    void shouldHandleBadCredentials() {
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleBadCredentials(new BadCredentialsException("credenciais inválidas"));

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("credenciais inválidas", response.getBody().getMessage());
        assertEquals(401, response.getBody().getStatus());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    @DisplayName("Deve tratar IllegalArgumentException com 400")
    void shouldHandleIllegalArgument() {
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleIllegalArgument(new IllegalArgumentException("argumento inválido"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("argumento inválido", response.getBody().getMessage());
    }

    @Test
    @DisplayName("Deve tratar IllegalStateException com 409")
    void shouldHandleIllegalState() {
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleIllegalState(new IllegalStateException("estado inválido"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("estado inválido", response.getBody().getMessage());
    }

    @Test
    @DisplayName("Deve tratar FileNotFoundException com 404")
    void shouldHandleFileNotFound() {
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleFileNotFound(new FileNotFoundException("arquivo.zip"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("arquivo.zip", response.getBody().getMessage());
    }

    @Test
    @DisplayName("Deve tratar MaxUploadSizeExceededException com 413")
    void shouldHandleMaxUploadSize() {
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleMaxSize(new MaxUploadSizeExceededException(500L));

        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, response.getStatusCode());
        assertTrue(response.getBody().getMessage().contains("500MB"));
    }

    @Test
    @DisplayName("Deve tratar MethodArgumentNotValidException agregando erros de campo")
    void shouldHandleValidation() {
        FieldError fieldError = new FieldError("registerRequest", "email", "não pode ser vazio");
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError));

        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<GlobalExceptionHandler.ValidationErrorResponse> response = handler.handleValidation(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Erro de Validação", response.getBody().getError());
        assertEquals("não pode ser vazio", response.getBody().getFieldErrors().get("email"));
    }

    @Test
    @DisplayName("Deve tratar exceção genérica com 500")
    void shouldHandleGeneralException() {
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleGeneral(new RuntimeException("falha inesperada"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().getMessage().contains("falha inesperada"));
    }
}
