package com.cloud.drive.service;

import com.cloud.drive.dto.share.CreateShareRequest;
import com.cloud.drive.dto.share.ShareResponse;
import com.cloud.drive.dto.FileResponseDto;
import com.cloud.drive.exception.ApiException;
import com.cloud.drive.model.FileEntity;
import com.cloud.drive.model.FileShare;
import com.cloud.drive.repository.FileRepository;
import com.cloud.drive.repository.FileShareRepository;
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
        share.setToken(UUID.randomUUID().toString());
        share.setPermission(req.getPermission() != null ? req.getPermission() : "VIEW");
        share.setCreatedAt(LocalDateTime.now());
        share.setExpiresAt(req.getExpiresAt());

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
        shareRepo.delete(share);
    }

    public List<ShareResponse> getFilesSharedWithMe(String userEmail) {
        return shareRepo.findBySharedWithEmail(userEmail).stream()
                .filter(s -> s.getExpiresAt() == null || s.getExpiresAt().isAfter(LocalDateTime.now()))
                .map(s -> {
                    String fileName = fileRepo.findById(s.getFileId())
                            .map(FileEntity::getOriginalFileName).orElse("(deleted)");
                    return toResponse(s, fileName);
                })
                .collect(Collectors.toList());
    }

    /** Resolves a public token and returns the file metadata (no auth required). */
    public FileResponseDto resolvePublicToken(String token) {
        FileShare share = shareRepo.findByToken(token)
                .orElseThrow(() -> new ApiException("Share link not found or expired", HttpStatus.NOT_FOUND));
        if (share.getExpiresAt() != null && share.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ApiException("Share link has expired", HttpStatus.GONE);
        }
        FileEntity file = fileRepo.findById(share.getFileId())
                .orElseThrow(() -> new ApiException("File not found", HttpStatus.NOT_FOUND));

        FileResponseDto dto = new FileResponseDto();
        dto.setId(file.getId());
        dto.setOriginalFileName(file.getOriginalFileName());
        dto.setSize(file.getSize());
        dto.setType(file.getType());
        dto.setCreatedAt(file.getCreatedAt());
        dto.setStarred(file.isStarred());
        try {
            dto.setUrl(blobStorage.generateSasUrlForBlob(file.getBlobFileName()));
        } catch (Exception ignored) {}
        return dto;
    }

    /** Streams a file accessed via a public share token (no auth required). */
    public FileEntity resolveTokenForStream(String token) {
        FileShare share = shareRepo.findByToken(token)
                .orElseThrow(() -> new ApiException("Share link not found or expired", HttpStatus.NOT_FOUND));
        if (share.getExpiresAt() != null && share.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ApiException("Share link has expired", HttpStatus.GONE);
        }
        return fileRepo.findById(share.getFileId())
                .orElseThrow(() -> new ApiException("File not found", HttpStatus.NOT_FOUND));
    }

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
        return r;
    }
}