package com.cloud.drive.service;

import com.cloud.drive.dto.share.CreateShareRequest;
import com.cloud.drive.dto.share.SharedFileResponse;
import com.cloud.drive.dto.share.ShareResponse;
import com.cloud.drive.dto.share.PublicShareResponse;
import com.cloud.drive.exception.ApiException;
import com.cloud.drive.model.FileEntity;
import com.cloud.drive.model.FileShare;
import com.cloud.drive.repository.FileRepository;
import com.cloud.drive.repository.FileShareRepository;
import com.cloud.drive.util.TokenGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ShareService {

    private final FileShareRepository shareRepo;
    private final FileRepository fileRepo;
    private final BlobStorageService blobStorage;

    @Value("${app.public-base-url:http://localhost:8082}")
    private String publicBaseUrl;

    public ShareService(FileShareRepository shareRepo, FileRepository fileRepo, BlobStorageService blobStorage) {
        this.shareRepo = shareRepo;
        this.fileRepo = fileRepo;
        this.blobStorage = blobStorage;
    }

    @Transactional
    public ShareResponse createShare(Long fileId, String ownerEmail, CreateShareRequest req) {
        FileEntity file = fileRepo.findById(fileId)
                .orElseThrow(() -> new ApiException("File not found", HttpStatus.NOT_FOUND));
        if (!file.getUserId().equals(ownerEmail)) {
            throw new ApiException("Access denied", HttpStatus.FORBIDDEN);
        }

        FileShare share = new FileShare();
        share.setFileId(fileId);
        share.setOwnerEmail(ownerEmail);
        share.setSharedWithEmail(req.getSharedWithEmail());
        share.setToken(TokenGenerator.generateToken());
        share.setPermission(req.getPermission() != null ? req.getPermission() : "VIEW");
        share.setCreatedAt(LocalDateTime.now());
        // Enforce 1-day expiration policy for all shares
        share.setExpiresAt(LocalDateTime.now().plusDays(1));

        return toResponse(shareRepo.save(share), file.getOriginalFileName());
    }

    public List<ShareResponse> getSharesForFile(Long fileId, String ownerEmail) {
        FileEntity file = fileRepo.findById(fileId)
                .orElseThrow(() -> new ApiException("File not found", HttpStatus.NOT_FOUND));
        if (!file.getUserId().equals(ownerEmail)) {
            throw new ApiException("Access denied", HttpStatus.FORBIDDEN);
        }
        return shareRepo.findByFileId(fileId).stream()
                .map(s -> toResponse(s, file.getOriginalFileName()))
                .collect(Collectors.toList());
    }

    @Transactional
    public void revokeShare(Long shareId, String ownerEmail) {
        FileShare share = shareRepo.findById(shareId)
                .orElseThrow(() -> new ApiException("Share not found", HttpStatus.NOT_FOUND));
        if (!share.getOwnerEmail().equals(ownerEmail)) {
            throw new ApiException("Access denied", HttpStatus.FORBIDDEN);
        }
        share.setRevokedAt(LocalDateTime.now());
        shareRepo.save(share);
    }

    public List<ShareResponse> getActiveSharesForFile(Long fileId, String ownerEmail) {
        FileEntity file = fileRepo.findById(fileId)
                .orElseThrow(() -> new ApiException("File not found", HttpStatus.NOT_FOUND));
        if (!file.getUserId().equals(ownerEmail)) {
            throw new ApiException("Access denied", HttpStatus.FORBIDDEN);
        }
        return shareRepo.findByFileId(fileId).stream()
                .filter(s -> s.getRevokedAt() == null)
                .map(s -> toResponse(s, file.getOriginalFileName()))
                .collect(Collectors.toList());
    }

    public List<SharedFileResponse> getFilesSharedWithMe(String userEmail) {
        return shareRepo.findBySharedWithEmail(userEmail).stream()
                .filter(s -> s.getRevokedAt() == null)
                .filter(s -> s.getExpiresAt() == null || s.getExpiresAt().isAfter(LocalDateTime.now()))
                .map(s -> {
                    FileEntity file = fileRepo.findById(s.getFileId()).orElse(null);
                    String fileName = file != null ? file.getOriginalFileName() : "(deleted)";
                    return toRecipientResponse(s, file, fileName);
                })
                .collect(Collectors.toList());
    }

    /** Resolves a public token and returns the file metadata (no auth required). */
    public PublicShareResponse resolvePublicToken(String token) {
        FileShare share = shareRepo.findByToken(token)
                .orElseThrow(() -> new ApiException("Share link not found or expired", HttpStatus.NOT_FOUND));
        validateShare(share);
        FileEntity file = availableFile(share);
        PublicShareResponse dto = new PublicShareResponse();
        dto.setId(file.getId());
        dto.setOriginalFileName(file.getOriginalFileName());
        dto.setSize(file.getSize());
        dto.setType(file.getType());
        dto.setCreatedAt(file.getCreatedAt());
        dto.setPermission(share.getPermission());
        return dto;
    }

    /**
     * Resolves a public share token and returns the share record (not the file).
     * The controller must check {@link FileShare#getPermission()} before allowing download.
     */
    public FileShare resolveTokenForStream(String token) {
        FileShare share = shareRepo.findByToken(token)
                .orElseThrow(() -> new ApiException("Share link not found or expired", HttpStatus.NOT_FOUND));
        validateShare(share);
        return share;
    }

    /** Retrieve the file entity referenced by a share record. */
    public FileEntity fileFor(FileShare share) {
        return availableFile(share);
    }

    public FileShare resolveRecipientShare(Long shareId, String recipientEmail) {
        FileShare share = shareRepo.findById(shareId)
                .orElseThrow(() -> new ApiException("Share not found", HttpStatus.NOT_FOUND));
        if (share.getSharedWithEmail() == null
                || !share.getSharedWithEmail().equalsIgnoreCase(recipientEmail)) {
            throw new ApiException("Access denied", HttpStatus.FORBIDDEN);
        }
        validateShare(share);
        availableFile(share);
        return share;
    }

    private FileEntity availableFile(FileShare share) {
        FileEntity file = fileRepo.findById(share.getFileId())
                .orElseThrow(() -> new ApiException("File not found", HttpStatus.NOT_FOUND));
        if (file.getDeletedAt() != null || !"ACTIVE".equals(file.getStatus())) {
            throw new ApiException("File is no longer available", HttpStatus.GONE);
        }
        return file;
    }

    private void validateShare(FileShare share) {
        if (share.getRevokedAt() != null) {
            throw new ApiException("Share link has been revoked", HttpStatus.GONE);
        }
        if (share.getExpiresAt() != null && !share.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new ApiException("Share link has expired", HttpStatus.GONE);
        }
    }

    /** Maps a share to the owner-facing response (includes token). */
    private ShareResponse toResponse(FileShare s, String fileName) {
        ShareResponse r = new ShareResponse();
        r.setId(s.getId());
        r.setFileId(s.getFileId());
        r.setFileName(fileName);
        r.setOwnerEmail(s.getOwnerEmail());
        r.setSharedWithEmail(s.getSharedWithEmail());
        r.setToken(s.getToken());
        r.setPermission(s.getPermission());
        r.setCreatedAt(s.getCreatedAt());
        r.setExpiresAt(s.getExpiresAt());
        r.setRevokedAt(s.getRevokedAt());
        r.setPublicLink(publicBaseUrl + "/public/" + s.getToken());
        return r;
    }

    /**
     * Maps a share to the recipient-facing response.
     * Intentionally omits the token so recipients cannot bypass permission checks
     * via the unauthenticated public-stream endpoint.
     */
    private SharedFileResponse toRecipientResponse(FileShare s, FileEntity file, String fileName) {
        SharedFileResponse r = new SharedFileResponse();
        r.setId(s.getId());
        r.setFileId(s.getFileId());
        r.setFileName(fileName);
        r.setOwnerEmail(s.getOwnerEmail());
        r.setPermission(s.getPermission());
        r.setCreatedAt(s.getCreatedAt());
        r.setExpiresAt(s.getExpiresAt());
        if (file != null) {
            r.setSize(file.getSize());
            r.setType(file.getType());
        }
        return r;
    }
}