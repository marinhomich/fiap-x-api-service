package com.fiapx.api.application.dto;

import com.fiapx.api.domain.entity.Video;
import com.fiapx.api.domain.entity.enums.VideoStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoResponseDTO {

    private Long id;
    private String originalFileName;
    private Long fileSize;
    private Integer frameCount;
    private VideoStatus status;
    private String errorMessage;
    private String downloadUrl;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;

    public static VideoResponseDTO fromEntity(Video video) {
        String downloadUrl = null;
        if (video.getStatus() == VideoStatus.CONCLUIDO && video.getZipPath() != null) {
            downloadUrl = "/api/videos/" + video.getId() + "/download";
        }

        return VideoResponseDTO.builder()
                .id(video.getId())
                .originalFileName(video.getOriginalFileName())
                .fileSize(video.getFileSize())
                .frameCount(video.getFrameCount())
                .status(video.getStatus())
                .errorMessage(video.getErrorMessage())
                .downloadUrl(downloadUrl)
                .createdAt(video.getCreatedAt())
                .processedAt(video.getProcessedAt())
                .build();
    }
}
