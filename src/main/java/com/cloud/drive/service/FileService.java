package com.cloud.drive.service;

import com.cloud.drive.dto.FileResponseDto;
import com.cloud.drive.exception.ApiException;
import com.cloud.drive.model.FileEntity;
import com.cloud.drive.repository.FileRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FileService {

    private final BlobStorageService blobStorageService;
    private final FileRepository fileRepository;
    private final SubscriptionService subscriptionService;

    public FileService(BlobStorageService blobStorageService,
                       FileRepository fileRepository,
                       SubscriptionService subscriptionService) {
        this.blobStorageService = blobStorageService;
        this.fileRepository = fileRepository;
        this.subscriptionService = subscriptionService;
    }

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

        return mapToDto(fileRepository.save(fileEntity));
    }

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

    private FileEntity findOwned(Long fileId, String userId) {
        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ApiException("File not found", HttpStatus.NOT_FOUND));
        if (!file.getUserId().equals(userId)) {
            throw new ApiException("Access denied", HttpStatus.FORBIDDEN);
        }
        return file;
    }

    private List<FileResponseDto> refreshAndMap(List<FileEntity> entities) {
        return entities.stream().map(entity -> {
            FileResponseDto dto = mapToDto(entity);
            if (entity.getBlobFileName() != null) {
                try {
                    dto.setUrl(blobStorageService.generateSasUrlForBlob(entity.getBlobFileName()));
                } catch (Exception ignored) {}
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
}
