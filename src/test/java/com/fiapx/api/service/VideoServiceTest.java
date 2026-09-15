package com.fiapx.api.service;

import com.fiapx.api.application.dto.VideoResponseDTO;
import com.fiapx.api.application.dto.VideoUploadResponseDTO;
import com.fiapx.api.application.service.VideoService;
import com.fiapx.api.domain.entity.User;
import com.fiapx.api.domain.entity.Video;
import com.fiapx.api.domain.entity.enums.UserRole;
import com.fiapx.api.domain.entity.enums.VideoStatus;
import com.fiapx.api.domain.repository.VideoRepository;
import com.fiapx.api.infrastructure.messaging.VideoEventPublisher;
import com.fiapx.api.infrastructure.messaging.dto.VideoProcessEvent;
import com.fiapx.api.infrastructure.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VideoServiceTest {

    @Mock
    private VideoRepository videoRepository;

    @Mock
    private StorageService storageService;

    @Mock
    private VideoEventPublisher eventPublisher;

    @InjectMocks
    private VideoService videoService;

    private User sampleUser;
    private Video sampleVideo;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .name("Michel")
                .email("michel@fiap.com.br")
                .role(UserRole.USER)
                .build();

        sampleVideo = Video.builder()
                .id(10L)
                .originalFileName("video_teste.mp4")
                .storedFileName("stored_video_teste.mp4")
                .filePath("uploads/stored_video_teste.mp4")
                .zipPath("outputs/frames_video_10.zip")
                .fileSize(1024L)
                .frameCount(30)
                .status(VideoStatus.CONCLUIDO)
                .user(sampleUser)
                .createdAt(LocalDateTime.now())
                .processedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Deve fazer upload de vídeo válido e publicar evento com sucesso")
    void shouldUploadVideoSuccessfully() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "video",
                "sample.mp4",
                "video/mp4",
                "dummy video content".getBytes()
        );

        when(storageService.storeFile(eq(file), anyString())).thenReturn("uploads/mocked_file.mp4");
        when(videoRepository.save(any(Video.class))).thenAnswer(invocation -> {
            Video v = invocation.getArgument(0);
            v.setId(10L);
            return v;
        });

        VideoUploadResponseDTO response = videoService.uploadVideo(file, sampleUser);

        assertNotNull(response);
        assertEquals(10L, response.getVideoId());
        assertEquals(VideoStatus.RECEBIDO, response.getStatus());
        verify(eventPublisher, times(1)).publishVideoProcessEvent(any(VideoProcessEvent.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar enviar arquivo com extensão inválida")
    void shouldThrowExceptionForInvalidFileExtension() {
        MockMultipartFile file = new MockMultipartFile(
                "video",
                "malicious.exe",
                "application/octet-stream",
                "fake content".getBytes()
        );

        assertThrows(IllegalArgumentException.class, () -> videoService.uploadVideo(file, sampleUser));
        verifyNoInteractions(storageService);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("Deve listar vídeos do usuário com paginação")
    void shouldListUserVideos() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Video> page = new PageImpl<>(List.of(sampleVideo));

        when(videoRepository.findByUserIdOrderByCreatedAtDesc(1L, pageable)).thenReturn(page);

        Page<VideoResponseDTO> result = videoService.listUserVideos(sampleUser, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(10L, result.getContent().get(0).getId());
        assertEquals("/api/videos/10/download", result.getContent().get(0).getDownloadUrl());
    }

    @Test
    @DisplayName("Deve obter detalhes do vídeo existente")
    void shouldGetVideoDetails() {
        when(videoRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(sampleVideo));

        VideoResponseDTO response = videoService.getVideoDetails(10L, sampleUser);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals(VideoStatus.CONCLUIDO, response.getStatus());
    }

    @Test
    @DisplayName("Deve lançar exceção ao buscar vídeo inexistente ou de outro usuário")
    void shouldThrowExceptionWhenVideoNotFound() {
        when(videoRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> videoService.getVideoDetails(99L, sampleUser));
    }

    @Test
    @DisplayName("Deve carregar Resource do arquivo ZIP para download")
    void shouldDownloadZipSuccessfully() throws IOException {
        when(videoRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(sampleVideo));
        Resource mockResource = new ByteArrayResource("dummy zip bytes".getBytes());
        when(storageService.loadAsResource("outputs/frames_video_10.zip")).thenReturn(mockResource);

        Resource resource = videoService.downloadZip(10L, sampleUser);

        assertNotNull(resource);
        assertTrue(resource.exists());
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar baixar ZIP de vídeo não concluído")
    void shouldThrowExceptionWhenVideoNotCompleted() {
        sampleVideo.setStatus(VideoStatus.PROCESSANDO);
        when(videoRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(sampleVideo));

        assertThrows(IllegalStateException.class, () -> videoService.downloadZip(10L, sampleUser));
    }
}
