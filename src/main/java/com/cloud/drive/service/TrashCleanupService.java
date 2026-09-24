package com.cloud.drive.service;

import com.cloud.drive.model.FileEntity;
import com.cloud.drive.repository.FileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class TrashCleanupService {

    private static final Logger log = LoggerFactory.getLogger(TrashCleanupService.class);
    private static final int RETENTION_DAYS = 30;

    private final FileRepository fileRepository;
    private final FileService fileService;

    public TrashCleanupService(FileRepository fileRepository, FileService fileService) {
        this.fileRepository = fileRepository;
        this.fileService = fileService;
    }

    @Scheduled(fixedDelayString = "${app.trash.cleanup-delay-ms:21600000}",
            initialDelayString = "${app.trash.cleanup-initial-delay-ms:120000}")
    public void cleanupExpiredTrash() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(RETENTION_DAYS);
        for (FileEntity file : fileRepository.findByDeletedAtBeforeAndStatusNot(cutoff, "PENDING")) {
            try {
                fileService.purgeExpiredTrash(file);
            } catch (RuntimeException ex) {
                log.error("trash_cleanup_failed fileId={} deletedAt={} blobKey={}",
                        file.getId(), file.getDeletedAt(), file.getBlobFileName(), ex);
            }
        }
    }
}
