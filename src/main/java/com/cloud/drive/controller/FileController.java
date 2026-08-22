package com.cloud.drive.controller;

import com.cloud.drive.dto.FileResponseDto;
import com.cloud.drive.dto.UploadTargetDto;
import com.cloud.drive.model.FileEntity;
import com.cloud.drive.security.FilenamePolicy;
import com.cloud.drive.service.FileService;
import com.cloud.drive.util.MimePolicy;
import com.cloud.drive.util.RangeSupport;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.util.List;
import java.util.Map;

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
            @RequestParam(value = "teamId", required = false) Long teamId,
            @AuthenticationPrincipal UserDetails userDetails) throws Exception {
        return new ResponseEntity<>(fileService.uploadFile(file, userDetails.getUsername(), teamId), HttpStatus.CREATED);
    }

    // ── two-phase direct-to-storage upload ────────────────────────────────

    /** Phase 1 — returns a write SAS URL the client PUTs to directly. */
    @PostMapping("/upload/begin")
    public ResponseEntity<UploadTargetDto> beginUpload(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        long size = ((Number) body.get("size")).longValue();
        String rawFileName = (String) body.get("rawFileName");
        Long teamId = body.containsKey("teamId") ? ((Number) body.get("teamId")).longValue() : null;
        
        // If teamId is provided, we should ideally verify membership here too, 
        // but FileService.beginUpload will need to handle it.
        return ResponseEntity.ok(fileService.beginUpload(userDetails.getUsername(), size, rawFileName, teamId));
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

    @GetMapping("/team/{teamId}")
    public ResponseEntity<List<FileResponseDto>> getTeamFiles(
            @PathVariable Long teamId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(fileService.getTeamFiles(teamId, userDetails.getUsername()));
    }

    /**
     * Stream file content with HTTP Range support (RFC 7233).
     *
     * <p>Returns 200 for full-content requests, 206 for partial-content (byte-range) requests.
     * Uses {@link StreamingResponseBody} to release the servlet request thread immediately,
     * allowing large file transfers without blocking the thread pool.</p>
     *
     * <p>Clients that seek in video/PDF (byte-range) and interrupted downloads
     * that resume from a specific offset are both supported.</p>
     */
    @GetMapping(value = "/{fileId}/stream", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<StreamingResponseBody> streamFile(
            @PathVariable Long fileId,
            @RequestHeader(value = "Range", required = false) String rangeHeader,
            @AuthenticationPrincipal UserDetails userDetails) throws IOException {

        FileEntity f = fileService.findOwnedForStream(fileId, userDetails.getUsername());
        long[] range = RangeSupport.parse(rangeHeader, f.getSize());

        String disposition = MimePolicy.shouldInline(f.getType())
                ? "inline"
                : "attachment; filename=\"" + FilenamePolicy.encodeFilename(f.getOriginalFileName()) + "\"";

        StreamingResponseBody body = out -> fileService.stream(f, range, out);

        long contentLength = range == null ? f.getSize() : range[1] - range[0] + 1;

        ResponseEntity.BodyBuilder builder = ResponseEntity
                .status(range == null ? HttpStatus.OK : HttpStatus.PARTIAL_CONTENT)
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .contentLength(contentLength);

        if (range != null) {
            builder.header(HttpHeaders.CONTENT_RANGE,
                    String.format("bytes %d-%d/%d", range[0], range[1], f.getSize()));
        }

        return builder.body(body);
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
