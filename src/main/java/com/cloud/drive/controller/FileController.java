package com.cloud.drive.controller;

import com.cloud.drive.dto.FileResponseDto;
import com.cloud.drive.dto.UploadTargetDto;
import com.cloud.drive.service.FileService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    // ── legacy multipart upload (retained for backward compatibility) ──────

    /**
     * @deprecated Use POST /upload/begin + direct PUT to Azure + POST /upload/{id}/commit instead.
     */
    @Deprecated
    @PostMapping("/upload")
    public ResponseEntity<FileResponseDto> uploadFile(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) throws Exception {
        return new ResponseEntity<>(fileService.uploadFile(file, userDetails.getUsername()), HttpStatus.CREATED);
    }

    // ── two-phase direct-to-storage upload ────────────────────────────────

    /** Phase 1 — returns a write SAS URL the client PUTs to directly. */
    @PostMapping("/upload/begin")
    public ResponseEntity<UploadTargetDto> beginUpload(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        long size = ((Number) body.get("size")).longValue();
        String rawFileName = (String) body.get("rawFileName");
        return ResponseEntity.ok(fileService.beginUpload(userDetails.getUsername(), size, rawFileName));
    }

    /** Phase 2 — client confirms upload; backend verifies blob and finalizes record. */
    @PostMapping("/upload/{fileId}/commit")
    public ResponseEntity<FileResponseDto> commitUpload(
            @PathVariable Long fileId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(fileService.commitUpload(fileId, userDetails.getUsername()));
    }

    // ── file queries ──────────────────────────────────────────────────────

    @GetMapping("/me")
    public ResponseEntity<List<FileResponseDto>> getMyFiles(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(fileService.getFilesByUser(userDetails.getUsername()));
    }

    @GetMapping("/starred")
    public ResponseEntity<List<FileResponseDto>> getStarredFiles(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(fileService.getStarredFiles(userDetails.getUsername()));
    }

    @GetMapping("/trash")
    public ResponseEntity<List<FileResponseDto>> getTrashFiles(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(fileService.getTrashFiles(userDetails.getUsername()));
    }

    @GetMapping("/{fileId}/stream")
    public void streamFile(
            @PathVariable Long fileId,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletResponse response) throws IOException {
        fileService.streamFile(fileId, userDetails.getUsername(), response);
    }

    // ── mutators ───────────────────────────────────────────────────────────

    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void> deleteFile(
            @PathVariable Long fileId,
            @AuthenticationPrincipal UserDetails userDetails) {
        fileService.deleteFile(fileId, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{fileId}/restore")
    public ResponseEntity<Void> restoreFile(
            @PathVariable Long fileId,
            @AuthenticationPrincipal UserDetails userDetails) {
        fileService.restoreFile(fileId, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{fileId}/permanent")
    public ResponseEntity<Void> permanentlyDeleteFile(
            @PathVariable Long fileId,
            @AuthenticationPrincipal UserDetails userDetails) {
        fileService.permanentlyDeleteFile(fileId, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{fileId}/star")
    public ResponseEntity<FileResponseDto> toggleStar(
            @PathVariable Long fileId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(fileService.toggleStar(fileId, userDetails.getUsername()));
    }
}
