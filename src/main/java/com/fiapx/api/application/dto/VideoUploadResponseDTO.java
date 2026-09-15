package com.fiapx.api.application.dto;

import com.fiapx.api.domain.entity.enums.VideoStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoUploadResponseDTO {
    private Long videoId;
    private String fileName;
    private VideoStatus status;
    private String message;
}
