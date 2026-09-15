package com.fiapx.api.domain.entity;

import com.fiapx.api.domain.entity.enums.VideoStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class VideoTest {

    @Test
    @DisplayName("onCreate deve definir createdAt e status padrão RECEBIDO quando ausente")
    void shouldSetDefaultsOnCreate() {
        Video video = new Video();
        video.setOriginalFileName("video.mp4");

        ReflectionTestUtils.invokeMethod(video, "onCreate");

        assertNotNull(video.getCreatedAt());
        assertEquals(VideoStatus.RECEBIDO, video.getStatus());
    }

    @Test
    @DisplayName("onCreate não deve sobrescrever o status já definido")
    void shouldNotOverrideExistingStatusOnCreate() {
        Video video = new Video();
        video.setOriginalFileName("video.mp4");
        video.setStatus(VideoStatus.PROCESSANDO);

        ReflectionTestUtils.invokeMethod(video, "onCreate");

        assertEquals(VideoStatus.PROCESSANDO, video.getStatus());
    }
}
