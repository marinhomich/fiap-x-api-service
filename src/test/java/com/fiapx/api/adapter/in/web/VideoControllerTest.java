package com.fiapx.api.adapter.in.web;

import com.fiapx.api.application.dto.VideoResponseDTO;
import com.fiapx.api.application.dto.VideoUploadResponseDTO;
import com.fiapx.api.application.service.VideoService;
import com.fiapx.api.domain.entity.User;
import com.fiapx.api.domain.entity.enums.UserRole;
import com.fiapx.api.domain.entity.enums.VideoStatus;
import com.fiapx.api.infrastructure.security.JwtAuthenticationFilter;
import com.fiapx.api.infrastructure.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VideoController.class)
@AutoConfigureMockMvc(addFilters = false)
class VideoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VideoService videoService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("michel@fiap.com.br")
                .name("Michel")
                .role(UserRole.USER)
                .build();
    }

    @Test
    @DisplayName("Deve fazer upload de vídeo via multipart e retornar 202 Accepted")
    void shouldUploadVideoEndpoint() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "video",
                "meu_video.mp4",
                "video/mp4",
                "dummy content".getBytes()
        );

        VideoUploadResponseDTO response = VideoUploadResponseDTO.builder()
                .videoId(1L)
                .fileName("meu_video.mp4")
                .status(VideoStatus.RECEBIDO)
                .message("Enfileirado com sucesso")
                .build();

        when(videoService.uploadVideo(any(), any())).thenReturn(response);

        mockMvc.perform(multipart("/api/videos").file(file))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.videoId").value(1))
                .andExpect(jsonPath("$.status").value("RECEBIDO"));
    }

    @Test
    @DisplayName("Deve listar vídeos do usuário via GET /api/videos")
    void shouldListVideosEndpoint() throws Exception {
        VideoResponseDTO videoDTO = VideoResponseDTO.builder()
                .id(1L)
                .originalFileName("video.mp4")
                .status(VideoStatus.CONCLUIDO)
                .frameCount(60)
                .createdAt(LocalDateTime.now())
                .build();

        when(videoService.listUserVideos(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(videoDTO)));

        mockMvc.perform(get("/api/videos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].status").value("CONCLUIDO"));
    }

    @Test
    @DisplayName("Deve fazer download do ZIP via GET /api/videos/{id}/download")
    void shouldDownloadZipEndpoint() throws Exception {
        ByteArrayResource resource = new ByteArrayResource("dummy zip".getBytes());
        when(videoService.downloadZip(eq(1L), any())).thenReturn(resource);

        mockMvc.perform(get("/api/videos/1/download"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"frames_video_1.zip\""));
    }

    @Test
    @DisplayName("Deve retornar os detalhes de um vídeo via GET /api/videos/{id}")
    void shouldGetVideoDetailsEndpoint() throws Exception {
        VideoResponseDTO videoDTO = VideoResponseDTO.builder()
                .id(1L)
                .originalFileName("video.mp4")
                .status(VideoStatus.PROCESSANDO)
                .createdAt(LocalDateTime.now())
                .build();

        when(videoService.getVideoDetails(eq(1L), any())).thenReturn(videoDTO);

        mockMvc.perform(get("/api/videos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("PROCESSANDO"));
    }
}
