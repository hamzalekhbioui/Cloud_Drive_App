package com.cloud.drive.service.admin;

import com.cloud.drive.dto.admin.file.*;
import com.cloud.drive.exception.ApiException;
import com.cloud.drive.model.FileEntity;
import com.cloud.drive.model.FileShare;
import com.cloud.drive.repository.*;
import com.cloud.drive.service.BlobStorageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminFileService {
    private final FileRepository fileRepository;
    private final FileShareRepository shareRepository;
    private final FileAiProcessingRepository aiRepository;
    private final TeamRepository teamRepository;
    private final BlobStorageService blobStorageService;

    public AdminFileService(FileRepository fileRepository,
                            FileShareRepository shareRepository,
                            FileAiProcessingRepository aiRepository,
                            TeamRepository teamRepository,
                            BlobStorageService blobStorageService) {
        this.fileRepository = fileRepository;
        this.shareRepository = shareRepository;
        this.aiRepository = aiRepository;
        this.teamRepository = teamRepository;
        this.blobStorageService = blobStorageService;
    }

    @Transactional(readOnly = true)
    public Page<AdminFileDto> listFiles(String owner, String status, String type,
                                        Long minSize, Long maxSize,
                                        LocalDateTime fromDate, LocalDateTime toDate,
                                        Pageable pageable) {
        return fileRepository.findAllForAdmin(blankToNull(owner), blankToNull(status),
                        blankToNull(type), minSize, maxSize, fromDate, toDate, pageable)
                .map(this::toFileDto);
    }

    @Transactional(readOnly = true)
    public AdminFileDto getFile(Long fileId) {
        return toFileDto(findFile(fileId));
    }

    @Transactional
    public AdminFileDto softDelete(Long fileId) {
        FileEntity file = findFile(fileId);
        file.setDeletedAt(file.getDeletedAt() == null ? LocalDateTime.now() : file.getDeletedAt());
        return toFileDto(fileRepository.save(file));
    }

    @Transactional
    public AdminFileDto restore(Long fileId) {
        FileEntity file = findFile(fileId);
        file.setDeletedAt(null);
        return toFileDto(fileRepository.save(file));
    }

    @Transactional
    public void purge(Long fileId) {
        FileEntity file = findFile(fileId);
        if (file.getBlobFileName() != null && !file.getBlobFileName().isBlank()) {
            blobStorageService.deleteFile(file.getBlobFileName());
        }
        aiRepository.deleteById(file.getId());
        shareRepository.deleteByFileId(file.getId());
        fileRepository.delete(file);
    }

    @Transactional(readOnly = true)
    public Page<AdminShareDto> listShares(String owner, Boolean revoked, Pageable pageable) {
        return shareRepository.findAllForAdmin(blankToNull(owner), revoked, pageable)
                .map(this::toShareDto);
    }

    @Transactional
    public AdminShareDto revokeShare(Long shareId) {
        FileShare share = shareRepository.findById(shareId)
                .orElseThrow(() -> new ApiException("Share not found", HttpStatus.NOT_FOUND));
        if (share.getRevokedAt() == null) share.setRevokedAt(LocalDateTime.now());
        return toShareDto(shareRepository.save(share));
    }

    private FileEntity findFile(Long fileId) {
        return fileRepository.findById(fileId)
                .orElseThrow(() -> new ApiException("File not found", HttpStatus.NOT_FOUND));
    }

    private AdminFileDto toFileDto(FileEntity file) {
        AdminFileDto dto = new AdminFileDto();
        dto.setId(file.getId());
        dto.setFileName(file.getOriginalFileName());
        dto.setOwnerEmail(file.getUserId());
        dto.setTeamId(file.getTeamId());
        if (file.getTeamId() != null) {
            teamRepository.findById(file.getTeamId()).ifPresent(team -> dto.setTeamName(team.getName()));
        }
        dto.setStatus(file.getStatus());
        dto.setType(file.getType());
        dto.setSize(file.getSize());
        dto.setCreatedAt(file.getCreatedAt());
        dto.setDeletedAt(file.getDeletedAt());
        dto.setBlobFileName(file.getBlobFileName());
        aiRepository.findById(file.getId()).ifPresent(ai -> dto.setAiStatus(ai.getStatus()));
        dto.setShares(shareRepository.findByFileIdOrderByCreatedAtDesc(file.getId()).stream().map(this::toShareDto).toList());
        return dto;
    }

    private AdminShareDto toShareDto(FileShare share) {
        AdminShareDto dto = new AdminShareDto();
        dto.setId(share.getId());
        dto.setFileId(share.getFileId());
        dto.setOwnerEmail(share.getOwnerEmail());
        dto.setSharedWithEmail(share.getSharedWithEmail());
        dto.setToken(share.getToken());
        dto.setPermission(share.getPermission());
        dto.setCreatedAt(share.getCreatedAt());
        dto.setExpiresAt(share.getExpiresAt());
        dto.setRevokedAt(share.getRevokedAt());
        fileRepository.findById(share.getFileId()).ifPresent(file -> dto.setFileName(file.getOriginalFileName()));
        return dto;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
