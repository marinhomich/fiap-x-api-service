package com.fiapx.api.infrastructure.standalone;

import com.fiapx.api.domain.entity.Video;
import com.fiapx.api.domain.entity.enums.VideoStatus;
import com.fiapx.api.domain.repository.VideoRepository;
import com.fiapx.api.infrastructure.messaging.dto.VideoProcessEvent;
import com.fiapx.api.infrastructure.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.io.FileSystemResource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StandaloneModeConfigTest {

    @TempDir
    Path tempDir;

    private VideoRepository videoRepository;
    private StorageService storageService;
    private StandaloneModeConfig config;

    @BeforeEach
    void setUp() {
        videoRepository = mock(VideoRepository.class);
        storageService = mock(StorageService.class);
        config = new StandaloneModeConfig(videoRepository, storageService);
    }

    @Test
    @DisplayName("Os beans dummy de ConnectionFactory devem responder com valores fixos e não quebrar")
    void dummyConnectionFactoryShouldExposeFixedValues() {
        var connectionFactory = config.standaloneConnectionFactory();

        assertNull(connectionFactory.createConnection());
        assertEquals("localhost", connectionFactory.getHost());
        assertEquals(5672, connectionFactory.getPort());
        assertEquals("/", connectionFactory.getVirtualHost());
        assertEquals("guest", connectionFactory.getUsername());
        assertTrue(connectionFactory.removeConnectionListener(null));
        assertDoesNotThrow(() -> {
            connectionFactory.addConnectionListener(null);
            connectionFactory.clearConnectionListeners();
        });
    }

    @Test
    @DisplayName("convertAndSend deve processar o vídeo localmente quando recebe um VideoProcessEvent")
    void convertAndSendShouldProcessVideoProcessEvent() throws Exception {
        Video video = Video.builder().id(1L).status(VideoStatus.RECEBIDO).build();
        when(videoRepository.findById(1L)).thenReturn(Optional.of(video));
        when(storageService.loadAsResource("")).thenReturn(new FileSystemResource(tempDir.toFile()));

        VideoProcessEvent event = VideoProcessEvent.builder().videoId(1L).timestamp(LocalDateTime.now()).build();

        RabbitTemplate rabbitTemplate = config.standaloneRabbitTemplate();
        rabbitTemplate.convertAndSend("video.exchange", "video.process.key", event);

        assertEquals(VideoStatus.CONCLUIDO, video.getStatus());
        assertEquals(10, video.getFrameCount());
        assertNotNull(video.getZipPath());
        assertTrue(Files.exists(tempDir.resolve(video.getZipPath())));
    }

    @Test
    @DisplayName("convertAndSend não deve processar quando o objeto não é um VideoProcessEvent")
    void convertAndSendShouldIgnoreOtherPayloads() {
        RabbitTemplate rabbitTemplate = config.standaloneRabbitTemplate();

        assertDoesNotThrow(() -> rabbitTemplate.convertAndSend("video.exchange", "video.process.key", "payload qualquer"));
    }

    @Test
    @DisplayName("processVideoStandalone deve retornar silenciosamente quando o vídeo não é encontrado")
    void shouldReturnEarlyWhenVideoNotFound() {
        when(videoRepository.findById(99L)).thenReturn(Optional.empty());

        VideoProcessEvent event = VideoProcessEvent.builder().videoId(99L).timestamp(LocalDateTime.now()).build();

        assertDoesNotThrow(() -> config.processVideoStandalone(event));
    }
}
