package com.fiapx.api.infrastructure.standalone;

import com.fiapx.api.domain.entity.Video;
import com.fiapx.api.domain.entity.enums.VideoStatus;
import com.fiapx.api.domain.repository.VideoRepository;
import com.fiapx.api.infrastructure.messaging.dto.VideoProcessEvent;
import com.fiapx.api.infrastructure.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Configuration
@Profile("standalone")
@EnableAsync
@RequiredArgsConstructor
public class StandaloneModeConfig {

    private final VideoRepository videoRepository;
    private final StorageService storageService;

    @Bean
    @Primary
    public ConnectionFactory standaloneConnectionFactory() {
        return new ConnectionFactory() {
            @Override public Connection createConnection() { return null; }
            @Override public String getHost() { return "localhost"; }
            @Override public int getPort() { return 5672; }
            @Override public String getVirtualHost() { return "/"; }
            @Override public String getUsername() { return "guest"; }
            @Override public void addConnectionListener(ConnectionListener listener) {}
            @Override public boolean removeConnectionListener(ConnectionListener listener) { return true; }
            @Override public void clearConnectionListeners() {}
        };
    }

    @Bean
    @Primary
    public RabbitTemplate standaloneRabbitTemplate() {
        return new RabbitTemplate(standaloneConnectionFactory()) {
            @Override
            public void convertAndSend(String exchange, String routingKey, Object object) {
                if (object instanceof VideoProcessEvent event) {
                    log.info("🎯 [MODO STANDALONE] Processando vídeo ID {} localmente em background sem RabbitMQ...", event.getVideoId());
                    processVideoStandalone(event);
                }
            }
        };
    }

    @Async
    public void processVideoStandalone(VideoProcessEvent event) {
        try {
            Thread.sleep(1500); // Simula tempo de fila

            Video video = videoRepository.findById(event.getVideoId()).orElse(null);
            if (video == null) return;

            video.setStatus(VideoStatus.PROCESSANDO);
            videoRepository.save(video);
            log.info("🎯 [MODO STANDALONE] Vídeo ID {} -> Status PROCESSANDO", video.getId());

            Thread.sleep(3000); // Simula processamento FFmpeg

            // Cria diretório temporário para frames
            Path tempDir = Files.createTempDirectory("standalone_frames_" + video.getId() + "_");
            List<File> frames = new ArrayList<>();

            for (int i = 1; i <= 10; i++) {
                File frame = tempDir.resolve(String.format("frame_%04d.png", i)).toFile();
                try (FileOutputStream fos = new FileOutputStream(frame)) {
                    fos.write(createDummyPng());
                }
                frames.add(frame);
            }

            // Compacta em .zip
            String zipName = String.format("frames_video_%d.zip", video.getId());
            Path zipPath = tempDir.resolve(zipName);

            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipPath.toFile()))) {
                for (File frame : frames) {
                    zos.putNextEntry(new ZipEntry(frame.getName()));
                    zos.write(Files.readAllBytes(frame.toPath()));
                    zos.closeEntry();
                }
            }

            // Salva no storage outputs/
            File zipFile = zipPath.toFile();
            Path dest = storageService.loadAsResource("").getFile().toPath().resolve("outputs").resolve(zipName);
            Files.createDirectories(dest.getParent());
            Files.copy(zipFile.toPath(), dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            video.setStatus(VideoStatus.CONCLUIDO);
            video.setZipPath("outputs/" + zipName);
            video.setFrameCount(frames.size());
            video.setProcessedAt(LocalDateTime.now());
            videoRepository.save(video);

            log.info("✅ [MODO STANDALONE] Vídeo ID {} CONCLUÍDO com sucesso! ZIP salvo em: outputs/{}", video.getId(), zipName);

        } catch (Exception e) {
            log.error("Erro no processamento standalone", e);
        }
    }

    private byte[] createDummyPng() {
        return new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
                0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
                0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, (byte) 0xC4, (byte) 0x89
        };
    }
}
