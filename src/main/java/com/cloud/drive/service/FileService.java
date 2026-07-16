package com.cloud.drive.service;

import com.cloud.drive.dto.FileResponseDto;
import com.cloud.drive.dto.UploadTargetDto;
import com.cloud.drive.exception.ApiException;
import com.cloud.drive.model.FileEntity;
import com.cloud.drive.repository.FileRepository;
import com.cloud.drive.storage.StorageService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FileService {

    private static final Logger log = LoggerFactory.getLogger(FileService.class);
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_PENDING = "PENDING";
    private static final long SAS_UPLOAD_TTL_SECONDS = 600;

    private final BlobStorageService blobStorageService;
    private final StorageService storageService;
    private final FileRepository fileRepository;
    private final SubscriptionService subscriptionService;

    public FileService(BlobStorageService blobStorageService,
                       StorageService storageService,
                       FileRepository fileRepository,
                       SubscriptionService subscriptionService) {
        this.blobStorageService = blobStorageService;
        this.storageService = storageService;
        this.fileRepository = fileRepository;
        this.subscriptionService = subscriptionService;
    }

    // ── legacy multipart upload (retained for backward compatibility) ──────

    /**
     * @deprecated Use {@link #beginUpload} + client-side direct-to-Azure PUT + {@link #commitUpload} instead.
     *             This method proxies the entire file body through the backend, wasting heap and bandwidth.
     */
    @Deprecated
    @Transactional
    public FileResponseDto uploadFile(MultipartFile file, String userId) throws IOException {
        subscriptionService.enforceStorageQuota(userId, file.getSize());
        String originalFileName = file.getOriginalFilename();
        String blobFileName = UUID.randomUUID().toString() + "-" + originalFileName;

        String sasUrl = blobStorageService.uploadFile(file, blobFileName);

        FileEntity fileEntity = new FileEntity();
        fileEntity.setOriginalFileName(originalFileName);
        fileEntity.setBlobFileName(blobFileName);
        fileEntity.setUrl(sasUrl);
        fileEntity.setSize(file.getSize());
        fileEntity.setType(file.getContentType());
        fileEntity.setUserId(userId);
        fileEntity.setCreatedAt(LocalDateTime.now());
        fileEntity.setStatus(STATUS_ACTIVE);

        return mapToDto(fileRepository.save(fileEntity));
    }

    // ── two-phase direct-to-storage upload ────────────────────────────────

    /**
     * Phase 1 — reserve quota, create a PENDING file record, and mint a short-lived
     * write SAS URL the client can PUT to directly (bypassing the backend entirely).
     */
    @Transactional
    public UploadTargetDto beginUpload(String userId, long declaredSize, String rawFileName) {
        subscriptionService.reserveQuota(userId, declaredSize);

        String safeName = sanitizeFileName(rawFileName);
        String ext = extension(rawFileName);
        String blobKey = userId + "/" + UUID.randomUUID() + ext;

        String writeUrl = storageService.createUploadTarget(blobKey,
                contentTypeFor(ext), declaredSize, Duration.ofSeconds(SAS_UPLOAD_TTL_SECONDS));

        FileEntity pending = new FileEntity();
        pending.setUserId(userId);
        pending.setOriginalFileName(safeName);
        pending.setBlobFileName(blobKey);
        pending.setSize(declaredSize);
        pending.setType(contentTypeFor(ext));
        pending.setStatus(STATUS_PENDING);
        pending.setCreatedAt(LocalDateTime.now());
        fileRepository.save(pending);

        return new UploadTargetDto(pending.getId(), writeUrl, blobKey, SAS_UPLOAD_TTL_SECONDS);
    }

    /**
     * Phase 2 — client tells us it finished uploading; we verify the blob's actual
     * size matches the declared size (anti-forgery) and finalize the record atomically.
     */
    @Transactional
    public FileResponseDto commitUpload(Long fileId, String userId) {
        FileEntity f = findOwned(fileId, userId);
        if (!STATUS_PENDING.equals(f.getStatus())) {
            throw new ApiException("Invalid commit — file is not in PENDING status", HttpStatus.CONFLICT);
        }
        storageService.assertLength(f.getBlobFileName(), f.getSize());
        f.setStatus(STATUS_ACTIVE);
        f.setUrl(storageService.createReadUrl(f.getBlobFileName(), false, Duration.ofMinutes(15)));
        return mapToDto(fileRepository.save(f));
    }

    // ── file queries ──────────────────────────────────────────────────────

    public List<FileResponseDto> getFilesByUser(String userId) {
        return refreshAndMap(fileRepository.findByUserIdAndDeletedAtIsNull(userId));
    }

    public List<FileResponseDto> getStarredFiles(String userId) {
        return refreshAndMap(fileRepository.findByUserIdAndStarredTrueAndDeletedAtIsNull(userId));
    }

    public List<FileResponseDto> getTrashFiles(String userId) {
        return refreshAndMap(fileRepository.findByUserIdAndDeletedAtIsNotNull(userId));
    }

    public void streamFile(Long fileId, String userId, HttpServletResponse response) throws IOException {
        FileEntity file = findOwned(fileId, userId);
        String contentType = (file.getType() != null) ? file.getType() : "application/octet-stream";
        response.setContentType(contentType);
        response.setHeader("Content-Disposition", "inline; filename=\"" + file.getOriginalFileName() + "\"");
        blobStorageService.streamToOutput(file.getBlobFileName(), response.getOutputStream());
    }

    /**
     * Returns the file entity owned by the given user, for range-aware streaming.
     */
    public FileEntity findOwnedForStream(Long fileId, String userId) {
        return findOwned(fileId, userId);
    }

    /**
     * Stream bytes from the underlying storage to the given output, supporting byte-range.
     *
     * @param file  the file entity
     * @param range two-element array [firstByte, lastByte] (inclusive), or {@code null} for the whole file
     * @param out   the servlet output stream
     */
    public void stream(FileEntity file, long[] range, OutputStream out) {
        storageService.streamTo(file.getBlobFileName(), range, out);
    }

    // ── mutators ──────────────────────────────────────────────────────────

    @Transactional
    public void deleteFile(Long fileId, String userId) {
        FileEntity file = findOwned(fileId, userId);
        file.setDeletedAt(LocalDateTime.now());
        fileRepository.save(file);
    }

    @Transactional
    public void restoreFile(Long fileId, String userId) {
        FileEntity file = findOwned(fileId, userId);
        file.setDeletedAt(null);
        fileRepository.save(file);
    }

    @Transactional
    public void permanentlyDeleteFile(Long fileId, String userId) {
        FileEntity file = findOwned(fileId, userId);
        blobStorageService.deleteFile(file.getBlobFileName());
        fileRepository.delete(file);
    }

    @Transactional
    public FileResponseDto toggleStar(Long fileId, String userId) {
        FileEntity file = findOwned(fileId, userId);
        file.setStarred(!file.isStarred());
        return mapToDto(fileRepository.save(file));
    }

    // ── private helpers ────────────────────────────────────────────────────

    private FileEntity findOwned(Long fileId, String userId) {
        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ApiException("File not found", HttpStatus.NOT_FOUND));
        if (!file.getUserId().equals(userId)) {
            throw new ApiException("Access denied", HttpStatus.FORBIDDEN);
        }
        return file;
    }

    private List<FileResponseDto> refreshAndMap(List<FileEntity> entities) {
        return entities.stream()
                .filter(e -> STATUS_ACTIVE.equals(e.getStatus()))
                .map(entity -> {
                    FileResponseDto dto = mapToDto(entity);
                    if (entity.getBlobFileName() != null) {
                        try {
                            dto.setUrl(blobStorageService.generateSasUrlForBlob(entity.getBlobFileName()));
                        } catch (Exception e) {
                            log.warn("Failed to generate SAS URL for blob {}", entity.getBlobFileName(), e);
                        }
                    }
                    return dto;
                }).collect(Collectors.toList());
    }

    private FileResponseDto mapToDto(FileEntity entity) {
        FileResponseDto dto = new FileResponseDto();
        dto.setId(entity.getId());
        dto.setOriginalFileName(entity.getOriginalFileName());
        dto.setUrl(entity.getUrl());
        dto.setSize(entity.getSize());
        dto.setType(entity.getType());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setStarred(entity.isStarred());
        dto.setDeletedAt(entity.getDeletedAt());
        return dto;
    }

    /**
     * Strips path traversal characters, null-bytes, and limits length.
     */
    private String sanitizeFileName(String raw) {
        if (raw == null || raw.isBlank()) return "unnamed";
        String safe = raw.replaceAll("[\\\\/]", "_")
                .replaceAll("\u0000", "")
                .trim();
        return safe.length() > 255 ? safe.substring(0, 255) : safe;
    }

    private String extension(String fileName) {
        if (fileName == null) return "";
        int dot = fileName.lastIndexOf('.');
        return (dot >= 0) ? fileName.substring(dot).toLowerCase() : "";
    }

    private String contentTypeFor(String ext) {
        return switch (ext) {
            case ".pdf" -> "application/pdf";
            case ".doc", ".docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case ".xls", ".xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case ".ppt", ".pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case ".png" -> "image/png";
            case ".jpg", ".jpeg" -> "image/jpeg";
            case ".gif" -> "image/gif";
            case ".svg" -> "image/svg+xml";
            case ".webp" -> "image/webp";
            case ".mp4" -> "video/mp4";
            case ".zip" -> "application/zip";
            case ".txt" -> "text/plain";
            case ".csv" -> "text/csv";
            case ".json" -> "application/json";
            default -> "application/octet-stream";
        };
    }
}
