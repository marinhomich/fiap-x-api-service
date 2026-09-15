package com.fiapx.api.infrastructure.storage;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Slf4j
@Service
public class LocalStorageService implements StorageService {

    @Value("${storage.base-path:./storage_data}")
    private String basePathStr;

    private Path basePath;

    @PostConstruct
    public void init() {
        this.basePath = Paths.get(basePathStr).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.basePath.resolve("uploads"));
            Files.createDirectories(this.basePath.resolve("outputs"));
            Files.createDirectories(this.basePath.resolve("temp"));
            log.info("Diretórios de armazenamento inicializados em: {}", this.basePath);
        } catch (IOException e) {
            log.error("Erro ao inicializar diretórios de armazenamento", e);
            throw new RuntimeException("Não foi possível inicializar diretórios de storage", e);
        }
    }

    @Override
    public String storeFile(MultipartFile file, String destinationFileName) throws IOException {
        Path targetLocation = this.basePath.resolve("uploads").resolve(destinationFileName);
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
        return "uploads/" + destinationFileName;
    }

    @Override
    public Resource loadAsResource(String relativeFilePath) throws IOException {
        Path filePath = this.basePath.resolve(relativeFilePath).normalize();
        Resource resource = new UrlResource(filePath.toUri());
        if (resource.exists() && resource.isReadable()) {
            return resource;
        } else {
            throw new FileNotFoundException("Arquivo não encontrado ou não legível: " + relativeFilePath);
        }
    }

    @Override
    public boolean exists(String relativeFilePath) {
        Path filePath = this.basePath.resolve(relativeFilePath).normalize();
        return Files.exists(filePath);
    }

    @Override
    public void delete(String relativeFilePath) {
        try {
            Path filePath = this.basePath.resolve(relativeFilePath).normalize();
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.warn("Não foi possível excluir arquivo: {}", relativeFilePath, e);
        }
    }
}
