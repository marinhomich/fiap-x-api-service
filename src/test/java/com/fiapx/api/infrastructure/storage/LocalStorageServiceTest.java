package com.fiapx.api.infrastructure.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LocalStorageServiceTest {

    @TempDir
    Path tempDir;

    private LocalStorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new LocalStorageService();
        ReflectionTestUtils.setField(storageService, "basePathStr", tempDir.toString());
        storageService.init();
    }

    @Test
    @DisplayName("init deve criar os diretórios uploads, outputs e temp")
    void shouldCreateBaseDirectoriesOnInit() {
        assertTrue(Files.isDirectory(tempDir.resolve("uploads")));
        assertTrue(Files.isDirectory(tempDir.resolve("outputs")));
        assertTrue(Files.isDirectory(tempDir.resolve("temp")));
    }

    @Test
    @DisplayName("storeFile deve salvar o arquivo em uploads/ e retornar o caminho relativo")
    void shouldStoreFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile("video", "video.mp4", "video/mp4", "conteúdo".getBytes());

        String relativePath = storageService.storeFile(file, "video_armazenado.mp4");

        assertEquals("uploads/video_armazenado.mp4", relativePath);
        assertTrue(Files.exists(tempDir.resolve("uploads").resolve("video_armazenado.mp4")));
    }

    @Test
    @DisplayName("loadAsResource deve retornar o recurso quando o arquivo existe")
    void shouldLoadExistingResource() throws Exception {
        Files.writeString(tempDir.resolve("outputs").resolve("frames.zip"), "zip-conteudo");

        Resource resource = storageService.loadAsResource("outputs/frames.zip");

        assertTrue(resource.exists());
        assertTrue(resource.isReadable());
    }

    @Test
    @DisplayName("loadAsResource deve lançar FileNotFoundException quando o arquivo não existe")
    void shouldThrowWhenResourceMissing() {
        assertThrows(FileNotFoundException.class, () -> storageService.loadAsResource("outputs/inexistente.zip"));
    }

    @Test
    @DisplayName("exists deve retornar true/false corretamente")
    void shouldCheckExistence() throws Exception {
        assertFalse(storageService.exists("outputs/nao_existe.zip"));

        Files.writeString(tempDir.resolve("outputs").resolve("existe.zip"), "conteudo");

        assertTrue(storageService.exists("outputs/existe.zip"));
    }

    @Test
    @DisplayName("delete deve remover o arquivo quando ele existe")
    void shouldDeleteExistingFile() throws Exception {
        Path file = tempDir.resolve("outputs").resolve("a_remover.zip");
        Files.writeString(file, "conteudo");

        storageService.delete("outputs/a_remover.zip");

        assertFalse(Files.exists(file));
    }

    @Test
    @DisplayName("delete não deve lançar exceção quando o arquivo não existe")
    void shouldNotThrowWhenDeletingMissingFile() {
        assertDoesNotThrow(() -> storageService.delete("outputs/nunca_existiu.zip"));
    }
}
