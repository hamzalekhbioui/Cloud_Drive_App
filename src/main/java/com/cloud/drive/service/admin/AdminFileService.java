package com.cloud.drive.service.admin;

import com.cloud.drive.dto.admin.file.*;
import com.cloud.drive.exception.ApiException;
import com.cloud.drive.model.FileEntity;
import com.cloud.drive.model.FileShare;
import com.cloud.drive.model.FileAiProcessing;
import com.cloud.drive.model.Team;
import com.cloud.drive.repository.*;
import com.cloud.drive.service.BlobStorageService;
import com.cloud.drive.security.admin.AdminPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AdminFileService {
    private final FileRepository fileRepository;
    private final FileShareRepository shareRepository;
    private final FileAiProcessingRepository aiRepository;
    private final TeamRepository teamRepository;
    private final BlobStorageService blobStorageService;
    private final AdminAuditService auditService;

    public AdminFileService(FileRepository fileRepository,
                            FileShareRepository shareRepository,
                            FileAiProcessingRepository aiRepository,
                            TeamRepository teamRepository,
                            BlobStorageService blobStorageService,
                            AdminAuditService auditService) {
        this.fileRepository = fileRepository;
        this.shareRepository = shareRepository;
        this.aiRepository = aiRepository;
        this.teamRepository = teamRepository;
        this.blobStorageService = blobStorageService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<AdminFileDto> listFiles(String owner, String status, String type,
                                        Long minSize, Long maxSize,
                                        LocalDateTime fromDate, LocalDateTime toDate,
                                        Pageable pageable) {
        Page<FileEntity> files = fileRepository.findAllForAdmin(blankToNull(owner), blankToNull(status),
                blankToNull(type), minSize, maxSize, fromDate, toDate, pageable);
        List<Long> fileIds = files.getContent().stream().map(FileEntity::getId).toList();
        List<Long> teamIds = files.getContent().stream().map(FileEntity::getTeamId)
                .filter(java.util.Objects::nonNull).distinct().toList();

        Map<Long, Team> teamsById = teamIds.isEmpty() ? Map.of() : teamRepository.findAllById(teamIds).stream()
                .collect(Collectors.toMap(Team::getId, Function.identity()));
        Map<Long, FileAiProcessing> aiByFileId = fileIds.isEmpty() ? Map.of() : aiRepository.findAllById(fileIds).stream()
                .collect(Collectors.toMap(FileAiProcessing::getFileId, Function.identity()));
        Map<Long, List<FileShare>> sharesByFileId = fileIds.isEmpty() ? Map.of()
                : shareRepository.findByFileIdInOrderByCreatedAtDesc(fileIds).stream()
                        .collect(Collectors.groupingBy(FileShare::getFileId));

        return files.map(file -> toFileDto(file,
                file.getTeamId() == null ? null : teamsById.get(file.getTeamId()),
                aiByFileId.get(file.getId()), sharesByFileId.getOrDefault(file.getId(), List.of())));
    }

    @Transactional(readOnly = true)
    public AdminFileDto getFile(Long fileId) {
        return toFileDto(findFile(fileId));
    }

    @Transactional
    public AdminFileDto softDelete(Long fileId) {
        return softDelete(fileId, null, null);
    }

    @Transactional
    public AdminFileDto softDelete(Long fileId, AdminPrincipal admin, String ip) {
        FileEntity file = findFile(fileId);
        record(admin, AdminAuditActions.FILE_DELETE, "FILE", fileId, "{\"fileId\":" + fileId + "}", ip);
        file.setDeletedAt(file.getDeletedAt() == null ? LocalDateTime.now() : file.getDeletedAt());
        return toFileDto(fileRepository.save(file));
    }

    @Transactional
    public AdminFileDto restore(Long fileId) {
        return restore(fileId, null, null);
    }

    @Transactional
    public AdminFileDto restore(Long fileId, AdminPrincipal admin, String ip) {
        FileEntity file = findFile(fileId);
        record(admin, AdminAuditActions.FILE_RESTORE, "FILE", fileId, "{\"fileId\":" + fileId + "}", ip);
        file.setDeletedAt(null);
        return toFileDto(fileRepository.save(file));
    }

    @Transactional
    public void purge(Long fileId) {
        purge(fileId, null, null);
    }

    @Transactional
    public void purge(Long fileId, AdminPrincipal admin, String ip) {
        FileEntity file = findFile(fileId);
        record(admin, AdminAuditActions.FILE_PURGE, "FILE", fileId, "{\"fileId\":" + fileId + "}", ip);
        if (file.getBlobFileName() != null && !file.getBlobFileName().isBlank()) {
            blobStorageService.deleteFile(file.getBlobFileName());
        }
        aiRepository.deleteById(file.getId());
        shareRepository.deleteByFileId(file.getId());
        fileRepository.delete(file);
    }

    @Transactional(readOnly = true)
    public Page<AdminShareDto> listShares(String owner, Boolean revoked, Pageable pageable) {
        Page<FileShare> shares = shareRepository.findAllForAdmin(blankToNull(owner), revoked, pageable);
        List<Long> fileIds = shares.getContent().stream().map(FileShare::getFileId).distinct().toList();
        Map<Long, FileEntity> filesById = fileIds.isEmpty() ? Map.of() : fileRepository.findAllById(fileIds).stream()
                .collect(Collectors.toMap(FileEntity::getId, Function.identity()));
        return shares.map(share -> toShareDto(share,
                filesById.containsKey(share.getFileId())
                        ? filesById.get(share.getFileId()).getOriginalFileName()
                        : null));
    }

    @Transactional
    public AdminShareDto revokeShare(Long shareId) {
        return revokeShare(shareId, null, null);
    }

    @Transactional
    public AdminShareDto revokeShare(Long shareId, AdminPrincipal admin, String ip) {
        FileShare share = shareRepository.findById(shareId)
                .orElseThrow(() -> new ApiException("Share not found", HttpStatus.NOT_FOUND));
        if (share.getRevokedAt() == null) {
            record(admin, AdminAuditActions.SHARE_REVOKE, "SHARE", shareId,
                    "{\"shareId\":" + shareId + ",\"fileId\":" + share.getFileId() + "}", ip);
            share.setRevokedAt(LocalDateTime.now());
        }
        return toShareDto(shareRepository.save(share));
    }

    private FileEntity findFile(Long fileId) {
        return fileRepository.findById(fileId)
                .orElseThrow(() -> new ApiException("File not found", HttpStatus.NOT_FOUND));
    }

    private AdminFileDto toFileDto(FileEntity file) {
        Team team = file.getTeamId() == null ? null : teamRepository.findById(file.getTeamId()).orElse(null);
        FileAiProcessing ai = aiRepository.findById(file.getId()).orElse(null);
        List<FileShare> shares = shareRepository.findByFileIdOrderByCreatedAtDesc(file.getId());
        return toFileDto(file, team, ai, shares);
    }

    private AdminFileDto toFileDto(FileEntity file, Team team, FileAiProcessing ai, List<FileShare> shares) {
        AdminFileDto dto = new AdminFileDto();
        dto.setId(file.getId());
        dto.setFileName(file.getOriginalFileName());
        dto.setOwnerEmail(file.getUserId());
        dto.setTeamId(file.getTeamId());
        if (team != null) dto.setTeamName(team.getName());
        dto.setStatus(file.getStatus());
        dto.setType(file.getType());
        dto.setSize(file.getSize());
        dto.setCreatedAt(file.getCreatedAt());
        dto.setDeletedAt(file.getDeletedAt());
        dto.setBlobFileName(file.getBlobFileName());
        if (ai != null) dto.setAiStatus(ai.getStatus());
        dto.setShares(shares.stream()
                .map(share -> toShareDto(share, file.getOriginalFileName()))
                .toList());
        return dto;
    }

    private AdminShareDto toShareDto(FileShare share) {
        String fileName = fileRepository.findById(share.getFileId())
                .map(FileEntity::getOriginalFileName).orElse(null);
        return toShareDto(share, fileName);
    }

    private AdminShareDto toShareDto(FileShare share, String fileName) {
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
        dto.setFileName(fileName);
        return dto;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private void record(AdminPrincipal admin, String action, String targetType, Long targetId,
                        String detail, String ip) {
        if (auditService != null && admin != null) {
            auditService.record(admin, action, targetType, String.valueOf(targetId), detail, ip);
        }
    }
}
