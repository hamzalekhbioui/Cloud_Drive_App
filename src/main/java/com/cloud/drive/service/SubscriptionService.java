package com.cloud.drive.service;

import com.cloud.drive.dto.subscription.ChangePlanRequest;
import com.cloud.drive.dto.subscription.SubscriptionResponse;
import com.cloud.drive.exception.ApiException;
import com.cloud.drive.model.Subscription;
import com.cloud.drive.repository.FileRepository;
import com.cloud.drive.repository.SubscriptionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class SubscriptionService {

    private final SubscriptionRepository subRepo;
    private final FileRepository fileRepo;

    public SubscriptionService(SubscriptionRepository subRepo, FileRepository fileRepo) {
        this.subRepo = subRepo;
        this.fileRepo = fileRepo;
    }

    /** Returns the user's subscription, creating a FREE one if none exists. */
    @Transactional
    public SubscriptionResponse getSubscription(String userEmail) {
        Subscription sub = subRepo.findByUserEmail(userEmail).orElseGet(() -> createFree(userEmail));
        return toResponse(sub, userEmail);
    }

    @Transactional
    public SubscriptionResponse changePlan(String userEmail, ChangePlanRequest req) {
        Subscription sub = subRepo.findByUserEmail(userEmail).orElseGet(() -> createFree(userEmail));

        if (sub.getPlan().equals(req.getPlan())) {
            throw new ApiException("Already on the " + req.getPlan() + " plan", HttpStatus.CONFLICT);
        }

        // Downgrade guard: ensure current usage fits in the new plan limit.
        Subscription probe = new Subscription();
        probe.setPlan(req.getPlan());
        long newLimit = probe.getPlanLimitBytes();
        long used = currentUsage(userEmail);
        if (used > newLimit) {
            throw new ApiException(
                    "Cannot downgrade: current storage usage exceeds the " + req.getPlan() + " plan limit.",
                    HttpStatus.CONFLICT);
        }

        sub.setPlan(req.getPlan());
        sub.setStartDate(LocalDateTime.now());
        sub.setEndDate(null);
        return toResponse(subRepo.save(sub), userEmail);
    }

    /** Throws 402 if uploading additionalBytes would exceed the plan quota. */
    public void enforceStorageQuota(String userEmail, long additionalBytes) {
        Subscription sub = subRepo.findByUserEmail(userEmail).orElseGet(() -> createFree(userEmail));
        long limit = sub.getPlanLimitBytes();
        long used  = currentUsage(userEmail);
        if (used + additionalBytes > limit) {
            throw new ApiException(
                    "Storage quota exceeded for the " + sub.getPlan() + " plan. Upgrade to upload more files.",
                    HttpStatus.PAYMENT_REQUIRED);
        }
    }

    public long getStorageLimitBytes(String userEmail) {
        return subRepo.findByUserEmail(userEmail)
                .map(Subscription::getPlanLimitBytes)
                .orElse(Subscription.FREE_BYTES);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Subscription createFree(String userEmail) {
        Subscription s = new Subscription();
        s.setUserEmail(userEmail);
        s.setPlan("FREE");
        s.setStatus("ACTIVE");
        s.setStartDate(LocalDateTime.now());
        return subRepo.save(s);
    }

    private long currentUsage(String userEmail) {
        Long total = fileRepo.sumSizeByUser(userEmail);
        return total != null ? total : 0L;
    }

    private SubscriptionResponse toResponse(Subscription sub, String userEmail) {
        long used  = currentUsage(userEmail);
        long limit = sub.getPlanLimitBytes();

        SubscriptionResponse r = new SubscriptionResponse();
        r.setPlan(sub.getPlan());
        r.setStatus(sub.getStatus());
        r.setStorageLimitBytes(limit);
        r.setStorageUsedBytes(used);
        r.setUsagePercent(limit > 0 ? (used * 100.0 / limit) : 0);
        r.setStartDate(sub.getStartDate());
        r.setEndDate(sub.getEndDate());
        return r;
    }
}