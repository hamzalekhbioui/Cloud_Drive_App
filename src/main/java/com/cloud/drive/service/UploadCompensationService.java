package com.cloud.drive.service;

import com.cloud.drive.repository.FileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Commits database compensation independently from the upload-commit transaction.
 * The caller rethrows the verification failure, so compensation cannot live in the
 * same transaction without being rolled back alongside that failure.
 */
@Service
public class UploadCompensationService {

    private final FileRepository fileRepository;
    private final SubscriptionService subscriptionService;

    public UploadCompensationService(FileRepository fileRepository,
                                     SubscriptionService subscriptionService) {
        this.fileRepository = fileRepository;
        this.subscriptionService = subscriptionService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void rejectPendingUpload(Long fileId, String userId, long reservedBytes) {
        fileRepository.deleteById(fileId);
        if (reservedBytes > 0) {
            subscriptionService.releaseQuota(userId, reservedBytes);
        }
    }
}
