package com.fiapx.api.bdd;

import com.fiapx.api.application.dto.VideoResponseDTO;
import com.fiapx.api.application.dto.VideoUploadResponseDTO;
import com.fiapx.api.application.service.VideoService;
import com.fiapx.api.domain.entity.User;
import com.fiapx.api.domain.entity.Video;
import com.fiapx.api.domain.entity.enums.UserRole;
import com.fiapx.api.domain.entity.enums.VideoStatus;
import com.fiapx.api.domain.repository.VideoRepository;
import com.fiapx.api.infrastructure.messaging.VideoEventPublisher;
import com.fiapx.api.infrastructure.storage.StorageService;
import io.cucumber.java.Before;
import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.E;
import io.cucumber.java.pt.Então;
import io.cucumber.java.pt.Quando;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class VideoStepDefinitions {

    private VideoService videoService;
    private VideoRepository videoRepository;
    private StorageService storageService;
    private VideoEventPublisher eventPublisher;

    private User currentUser;
    private VideoUploadResponseDTO uploadResponse;
    private Exception caughtException;
    private Page<VideoResponseDTO> listedVideos;

    @Before
    public void setup() {
        videoRepository = mock(VideoRepository.class);
        storageService = mock(StorageService.class);
        eventPublisher = mock(VideoEventPublisher.class);
        videoService = new VideoService(videoRepository, storageService, eventPublisher);
    }

    @Dado("que o usuário está autenticado com o e-mail {string}")
    public void usuarioAutenticado(String email) {
        currentUser = User.builder()
                .id(1L)
                .name("Michel")
                .email(email)
                .role(UserRole.USER)
                .build();
    }

    @Quando("o usuário envia um arquivo de vídeo {string} com formato válido")
    public void usuarioEnviaVideoValido(String fileName) throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "video",
                fileName,
                "video/mp4",
                "video data".getBytes()
        );

        when(storageService.storeFile(eq(file), anyString())).thenReturn("uploads/" + fileName);
        when(videoRepository.save(any(Video.class))).thenAnswer(inv -> {
            Video v = inv.getArgument(0);
            v.setId(100L);
            return v;
        });

        uploadResponse = videoService.uploadVideo(file, currentUser);
    }

    @Então("o sistema deve aceitar a requisição com status HTTP 202")
    public void sistemaAceitaRequisicao() {
        assertNotNull(uploadResponse);
        assertNotNull(uploadResponse.getVideoId());
    }

    @E("o vídeo deve ser registrado com status {string}")
    public void videoRegistradoComStatus(String statusStr) {
        assertEquals(VideoStatus.valueOf(statusStr), uploadResponse.getStatus());
    }

    @E("uma mensagem de processamento deve ser enviada para a fila do RabbitMQ")
    public void mensagemEnviadaParaRabbitMQ() {
        verify(eventPublisher, times(1)).publishVideoProcessEvent(any());
    }

    @Quando("o usuário envia um arquivo inválido {string}")
    public void usuarioEnviaArquivoInvalido(String fileName) {
        MockMultipartFile file = new MockMultipartFile(
                "video",
                fileName,
                "application/octet-stream",
                "malicious data".getBytes()
        );

        try {
            uploadResponse = videoService.uploadVideo(file, currentUser);
        } catch (Exception ex) {
            caughtException = ex;
        }
    }

    @Então("o sistema deve rejeitar a requisição informando formato não suportado")
    public void sistemaRejeitaRequisicao() {
        assertNotNull(caughtException);
        assertTrue(caughtException instanceof IllegalArgumentException);
    }

    @Dado("que o usuário possui um vídeo cadastrado com status {string}")
    public void usuarioPossuiVideoCadastrado(String statusStr) {
        currentUser = User.builder()
                .id(1L)
                .email("michel@fiap.com.br")
                .role(UserRole.USER)
                .build();

        Video video = Video.builder()
                .id(200L)
                .originalFileName("meu_video.mp4")
                .storedFileName("stored_meu_video.mp4")
                .filePath("uploads/stored_meu_video.mp4")
                .zipPath("outputs/frames_200.zip")
                .status(VideoStatus.valueOf(statusStr))
                .frameCount(50)
                .user(currentUser)
                .createdAt(LocalDateTime.now())
                .build();

        when(videoRepository.findByUserIdOrderByCreatedAtDesc(eq(1L), any()))
                .thenReturn(new PageImpl<>(List.of(video)));
    }

    @Quando("o usuário solicita a listagem dos seus vídeos")
    public void usuarioSolicitaListagem() {
        listedVideos = videoService.listUserVideos(currentUser, PageRequest.of(0, 10));
    }

    @Então("o sistema deve retornar a lista contendo o vídeo e a URL para download do ZIP")
    public void sistemaRetornaListaComUrlDownload() {
        assertNotNull(listedVideos);
        assertEquals(1, listedVideos.getTotalElements());
        VideoResponseDTO dto = listedVideos.getContent().get(0);
        assertEquals(VideoStatus.CONCLUIDO, dto.getStatus());
        assertEquals("/api/videos/200/download", dto.getDownloadUrl());
    }
}
