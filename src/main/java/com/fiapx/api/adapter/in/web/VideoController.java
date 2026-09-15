package com.fiapx.api.adapter.in.web;

import com.fiapx.api.application.dto.VideoResponseDTO;
import com.fiapx.api.application.dto.VideoUploadResponseDTO;
import com.fiapx.api.application.service.VideoService;
import com.fiapx.api.domain.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
@Tag(name = "Vídeos", description = "Endpoints para upload, acompanhamento de status e download de vídeos")
@SecurityRequirement(name = "BearerAuth")
public class VideoController {

    private final VideoService videoService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload de vídeo para processamento assíncrono", description = "Recebe um arquivo de vídeo (.mp4, .avi, .mov, .mkv), armazena e enfileira para extração de frames.")
    public ResponseEntity<VideoUploadResponseDTO> uploadVideo(
            @RequestParam("video") MultipartFile file,
            @AuthenticationPrincipal User user
    ) throws IOException {
        VideoUploadResponseDTO response = videoService.uploadVideo(file, user);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping
    @Operation(summary = "Listar vídeos do usuário", description = "Retorna a listagem paginada dos vídeos pertencentes ao usuário autenticado com status atualizado.")
    public ResponseEntity<Page<VideoResponseDTO>> listVideos(
            @AuthenticationPrincipal User user,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(videoService.listUserVideos(user, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obter detalhes de um vídeo", description = "Retorna os detalhes e status de processamento de um vídeo específico.")
    public ResponseEntity<VideoResponseDTO> getVideoDetails(
            @PathVariable Long id,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(videoService.getVideoDetails(id, user));
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Download do arquivo ZIP com os frames extraídos", description = "Permite o download do arquivo .zip contendo todos os frames em formato PNG.")
    public ResponseEntity<Resource> downloadZip(
            @PathVariable Long id,
            @AuthenticationPrincipal User user
    ) throws IOException {
        Resource resource = videoService.downloadZip(id, user);

        String filename = "frames_video_" + id + ".zip";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }
}
