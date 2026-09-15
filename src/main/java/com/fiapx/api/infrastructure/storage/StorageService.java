package com.fiapx.api.infrastructure.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface StorageService {
    String storeFile(MultipartFile file, String destinationFileName) throws IOException;
    Resource loadAsResource(String filePath) throws IOException;
    boolean exists(String filePath);
    void delete(String filePath);
}
