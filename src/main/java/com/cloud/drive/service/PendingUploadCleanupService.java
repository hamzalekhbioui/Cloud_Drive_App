package com.cloud.drive.service;

import com.cloud.drive.model.FileEntity;
import com.cloud.drive.repository.FileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class PendingUploadCleanupService {

    private static final Logger log = LoggerFactory.getLogger(PendingUploadCleanupService.class);

    private final FileRepository fileRepository;
    private final FileService fileService;

    public PendingUploadCleanupService(FileRepository fileRepository, FileService fileService) {
        this.fileRepository = fileRepository;
        this.fileService = fileService;
    }

    @Scheduled(fixedDelayString = "${app.upload.cleanup-delay-ms:60000}")
    public void cleanupExpiredUploads() {
        for (FileEntity file : fileRepository.findByStatusAndUploadExpiresAtBefore("PENDING", LocalDateTime.now())) {
            try {
                fileService.expirePendingUpload(file);
            } catch (RuntimeException ex) {
                log.error("pending_upload_cleanup_failed fileId={} blobKey={}",
                        file.getId(), file.getBlobFileName(), ex);
            }
        }
    }
}
