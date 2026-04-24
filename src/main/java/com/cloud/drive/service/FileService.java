package com.cloud.drive.service;

import com.cloud.drive.dto.FileResponseDto;
import com.cloud.drive.model.FileEntity;
import com.cloud.drive.repository.FileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FileService {

    private final BlobStorageService blobStorageService;
    private final FileRepository fileRepository;

    public FileService(BlobStorageService blobStorageService, FileRepository fileRepository) {
        this.blobStorageService = blobStorageService;
        this.fileRepository = fileRepository;
    }

    @Transactional
    public FileResponseDto uploadFile(MultipartFile file, String userId) throws IOException {
        String originalFileName = file.getOriginalFilename();
        String blobFileName = UUID.randomUUID().toString() + "-" + originalFileName;

        // Upload to Azure
        String sasUrl = blobStorageService.uploadFile(file, blobFileName);

        // Save metadata
        FileEntity fileEntity = new FileEntity();
        fileEntity.setOriginalFileName(originalFileName);
        fileEntity.setBlobFileName(blobFileName);
        fileEntity.setUrl(sasUrl);
        fileEntity.setSize(file.getSize());
        fileEntity.setType(file.getContentType());
        fileEntity.setUserId(userId);
        fileEntity.setCreatedAt(LocalDateTime.now());

        FileEntity savedEntity = fileRepository.save(fileEntity);
        return mapToDto(savedEntity);
    }

    public List<FileResponseDto> getFilesByUser(String userId) {
        return fileRepository.findByUserId(userId).stream().map(entity -> {
            // Refresh SAS URL if needed, but for now we generate it fresh
            String freshSasUrl = blobStorageService.generateSasUrlForBlob(entity.getBlobFileName());
            entity.setUrl(freshSasUrl);
            return mapToDto(entity);
        }).collect(Collectors.toList());
    }

    @Transactional
    public void deleteFile(Long fileId) {
        FileEntity fileEntity = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found with id: " + fileId));

        // Delete from Azure
        blobStorageService.deleteFile(fileEntity.getBlobFileName());

        // Delete metadata
        fileRepository.delete(fileEntity);
    }

    private FileResponseDto mapToDto(FileEntity entity) {
        FileResponseDto dto = new FileResponseDto();
        dto.setId(entity.getId());
        dto.setOriginalFileName(entity.getOriginalFileName());
        dto.setUrl(entity.getUrl());
        dto.setSize(entity.getSize());
        dto.setType(entity.getType());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }
}
