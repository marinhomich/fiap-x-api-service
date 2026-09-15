package com.fiapx.api.application.service;

import com.fiapx.api.application.dto.VideoResponseDTO;
import com.fiapx.api.application.dto.VideoUploadResponseDTO;
import com.fiapx.api.domain.entity.User;
import com.fiapx.api.domain.entity.Video;
import com.fiapx.api.domain.entity.enums.VideoStatus;
import com.fiapx.api.domain.repository.VideoRepository;
import com.fiapx.api.infrastructure.messaging.VideoEventPublisher;
import com.fiapx.api.infrastructure.messaging.dto.VideoProcessEvent;
import com.fiapx.api.infrastructure.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoService {

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(".mp4", ".avi", ".mov", ".mkv", ".wmv", ".flv", ".webm");

    private final VideoRepository videoRepository;
    private final StorageService storageService;
    private final VideoEventPublisher eventPublisher;

    @Transactional
    public VideoUploadResponseDTO uploadVideo(MultipartFile file, User user) throws IOException {
        validateVideoFile(file);

        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String storedFileName = String.format("%s_%s_%s%s", timestamp, UUID.randomUUID().toString().substring(0, 8), sanitizeFileName(originalFilename), extension);

        String storedRelativePath = storageService.storeFile(file, storedFileName);

        Video video = Video.builder()
                .originalFileName(originalFilename)
                .storedFileName(storedFileName)
                .filePath(storedRelativePath)
                .fileSize(file.getSize())
                .status(VideoStatus.RECEBIDO)
                .user(user)
                .build();

        Video savedVideo = videoRepository.save(video);
        log.info("Vídeo salvo no banco com ID: {} para o usuário: {}", savedVideo.getId(), user.getEmail());

        VideoProcessEvent event = VideoProcessEvent.builder()
                .videoId(savedVideo.getId())
                .userId(user.getId())
                .userEmail(user.getEmail())
                .originalFileName(savedVideo.getOriginalFileName())
                .storedFileName(savedVideo.getStoredFileName())
                .filePath(savedVideo.getFilePath())
                .fileSize(savedVideo.getFileSize())
                .timestamp(LocalDateTime.now())
                .build();

        eventPublisher.publishVideoProcessEvent(event);

        return VideoUploadResponseDTO.builder()
                .videoId(savedVideo.getId())
                .fileName(savedVideo.getOriginalFileName())
                .status(savedVideo.getStatus())
                .message("Vídeo recebido com sucesso e enfileirado para processamento assíncrono.")
                .build();
    }

    @Transactional(readOnly = true)
    public Page<VideoResponseDTO> listUserVideos(User user, Pageable pageable) {
        return videoRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable)
                .map(VideoResponseDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public VideoResponseDTO getVideoDetails(Long videoId, User user) {
        Video video = videoRepository.findByIdAndUserId(videoId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Vídeo não encontrado ou acesso não autorizado"));
        return VideoResponseDTO.fromEntity(video);
    }

    @Transactional(readOnly = true)
    public Resource downloadZip(Long videoId, User user) throws IOException {
        Video video = videoRepository.findByIdAndUserId(videoId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Vídeo não encontrado ou acesso não autorizado"));

        if (video.getStatus() != VideoStatus.CONCLUIDO || video.getZipPath() == null) {
            throw new IllegalStateException("O arquivo ZIP ainda não está pronto para download. Status atual: " + video.getStatus());
        }

        return storageService.loadAsResource(video.getZipPath());
    }

    private void validateVideoFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("O arquivo de vídeo não pode ser vazio");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !isValidExtension(filename)) {
            throw new IllegalArgumentException("Formato de arquivo não suportado. Extensões válidas: " + ALLOWED_EXTENSIONS);
        }
    }

    private boolean isValidExtension(String filename) {
        String ext = getFileExtension(filename).toLowerCase();
        return ALLOWED_EXTENSIONS.contains(ext);
    }

    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf(".");
        if (lastDotIndex == -1) {
            return "";
        }
        return filename.substring(lastDotIndex);
    }

    private String sanitizeFileName(String originalFilename) {
        String nameWithoutExt = originalFilename;
        int lastDot = originalFilename.lastIndexOf(".");
        if (lastDot != -1) {
            nameWithoutExt = originalFilename.substring(0, lastDot);
        }
        return nameWithoutExt.replaceAll("[^a-zA-Z0-9_-]", "_");
    }
}
