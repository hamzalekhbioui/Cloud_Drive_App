package com.cloud.drive.service;

import com.cloud.drive.dto.FileResponseDto;
import com.cloud.drive.dto.UploadTargetDto;
import com.cloud.drive.exception.ApiException;
import com.cloud.drive.model.FileEntity;
import com.cloud.drive.repository.FileRepository;
import com.cloud.drive.repository.TeamMemberRepository;
import com.cloud.drive.security.FilenamePolicy;
import com.cloud.drive.storage.StorageService;
import com.cloud.drive.util.MimePolicy;
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
    private final TeamMemberRepository teamMemberRepository;
    private final SubscriptionService subscriptionService;

    public FileService(BlobStorageService blobStorageService,
                       StorageService storageService,
                       FileRepository fileRepository,
                       TeamMemberRepository teamMemberRepository,
                       SubscriptionService subscriptionService) {
        this.blobStorageService = blobStorageService;
        this.storageService = storageService;
        this.fileRepository = fileRepository;
        this.teamMemberRepository = teamMemberRepository;
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

        // sanitise + validate extension before accepting
        String safeName = FilenamePolicy.sanitize(file.getOriginalFilename());
        FilenamePolicy.extension(safeName);   // throws 400 if denied/unknown

        String blobFileName = UUID.randomUUID().toString() + "-" + safeName;

        // P2.1 — detect MIME from magic bytes, never trust client Content-Type
        String detectedType = MimePolicy.detect(file.getInputStream(), safeName);

        String sasUrl = blobStorageService.uploadFile(file, blobFileName, detectedType);

        FileEntity fileEntity = new FileEntity();
        fileEntity.setOriginalFileName(safeName);
        fileEntity.setBlobFileName(blobFileName);
        fileEntity.setUrl(sasUrl);
        fileEntity.setSize(file.getSize());
        fileEntity.setType(detectedType);
        fileEntity.setUserId(userId);
        fileEntity.setCreatedAt(LocalDateTime.now());
        fileEntity.setStatus(STATUS_ACTIVE);

        return mapToDto(fileRepository.save(fileEntity));
    }

    @Transactional
    public FileResponseDto uploadFile(MultipartFile file, String userId, Long teamId) throws Exception {
        if (teamId != null) {
            requireTeamMembership(teamId, userId);
        }
        
        subscriptionService.reserveQuota(userId, file.getSize());

        String safeName = FilenamePolicy.sanitize(file.getOriginalFilename());
        String ext = FilenamePolicy.extension(safeName);
        String blobFileName = userId + "/" + UUID.randomUUID() + ext;

        // P2.1 — detect MIME from magic bytes, never trust client Content-Type
        String detectedType = MimePolicy.detect(file.getInputStream(), safeName);

        String sasUrl = blobStorageService.uploadFile(file, blobFileName, detectedType);

        FileEntity fileEntity = new FileEntity();
        fileEntity.setOriginalFileName(safeName);
        fileEntity.setBlobFileName(blobFileName);
        fileEntity.setUrl(sasUrl);
        fileEntity.setSize(file.getSize());
        fileEntity.setType(detectedType);
        fileEntity.setUserId(userId);
        fileEntity.setTeamId(teamId);
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
    public UploadTargetDto beginUpload(String userId, long declaredSize, String rawFileName, Long teamId) {
        if (teamId != null) {
            requireTeamMembership(teamId, userId);
        }
        
        subscriptionService.reserveQuota(userId, declaredSize);

        String safeName = FilenamePolicy.sanitize(rawFileName);
        String ext = FilenamePolicy.extension(safeName);      // throws 400 if denied/unknown
        String blobKey = userId + "/" + UUID.randomUUID() + ext;

        String contentType = FilenamePolicy.contentTypeFor(ext);
        // P2.1 — also validate against MimePolicy allow-list for defense-in-depth
        MimePolicy.validateType(contentType);

        String writeUrl = storageService.createUploadTarget(blobKey,
                contentType, declaredSize, Duration.ofSeconds(SAS_UPLOAD_TTL_SECONDS));

        FileEntity pending = new FileEntity();
        pending.setUserId(userId);
        pending.setTeamId(teamId);
        pending.setOriginalFileName(safeName);
        pending.setBlobFileName(blobKey);
        pending.setSize(declaredSize);
        pending.setType(contentType);
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

    public List<FileResponseDto> getTeamFiles(Long teamId, String userId) {
        requireTeamMembership(teamId, userId);
        return refreshAndMap(fileRepository.findActiveByTeamId(teamId));
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
        // Release quota so the usedBytes counter stays accurate
        if (file.getSize() != null && file.getSize() > 0) {
            subscriptionService.releaseQuota(userId, file.getSize());
        }
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
        
        boolean isOwner = file.getUserId().equals(userId);
        boolean isTeamMember = false;
        if (file.getTeamId() != null) {
            isTeamMember = teamMemberRepository.findByTeamIdAndUserEmail(file.getTeamId(), userId)
                    .map(m -> "ACTIVE".equals(m.getStatus()))
                    .orElse(false);
        }

        if (!isOwner && !isTeamMember) {
            throw new ApiException("Access denied", HttpStatus.FORBIDDEN);
        }
        
        // PENDING files should only be visible/committable by the owner or team admins
        if (STATUS_PENDING.equals(file.getStatus()) && !isOwner) {
            // Check if user is a team admin if it's a team file
            boolean isTeamAdmin = false;
            if (file.getTeamId() != null) {
                isTeamAdmin = teamMemberRepository.findByTeamIdAndUserEmail(file.getTeamId(), userId)
                        .map(m -> "ACTIVE".equals(m.getStatus()) && "ADMIN".equals(m.getRole()))
                        .orElse(false);
            }
            if (!isTeamAdmin) {
                throw new ApiException("File upload is still in progress", HttpStatus.FORBIDDEN);
            }
        }

        return file;
    }

    private void requireTeamMembership(Long teamId, String userId) {
        teamMemberRepository.findByTeamIdAndUserEmail(teamId, userId)
                .filter(m -> "ACTIVE".equals(m.getStatus()))
                .orElseThrow(() -> new ApiException("Not a member of this team", HttpStatus.FORBIDDEN));
    }

    private List<FileResponseDto> refreshAndMap(List<FileEntity> entities) {
        return entities.stream()
                .filter(e -> STATUS_ACTIVE.equals(e.getStatus()) || e.getStatus() == null)
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
        dto.setTeamId(entity.getTeamId());
        dto.setDeletedAt(entity.getDeletedAt());
        dto.setUserId(entity.getUserId());
        return dto;
    }

    // sanitizeFileName(), extension(), contentTypeFor() have been replaced by
    // FilenamePolicy — a centralised, security-hardened utility in the security package.
}
