package com.cloud.drive.service;

import com.cloud.drive.dto.subscription.ChangePlanRequest;
import com.cloud.drive.dto.subscription.SubscriptionResponse;
import com.cloud.drive.exception.ApiException;
import com.cloud.drive.model.Subscription;
import com.cloud.drive.repository.FileRepository;
import com.cloud.drive.repository.SubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);

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
        if (sub.getUsedBytes() > newLimit) {
            throw new ApiException(
                    "Cannot downgrade: current storage usage exceeds the " + req.getPlan() + " plan limit.",
                    HttpStatus.CONFLICT);
        }

        sub.setPlan(req.getPlan());
        sub.setStartDate(LocalDateTime.now());
        sub.setEndDate(null);
        return toResponse(subRepo.save(sub), userEmail);
    }

    // ── atomic quota operations ─────────────────────────────────────────────

    /**
     * Legacy multipart upload path — acquires a pessimistic row lock, checks
     * the counter, and reserves in one atomic step.
     *
     * <p>Must be called from within a {@code @Transactional} context (the caller's
     * transaction holds the lock until commit).</p>
     */
    @Transactional
    public void enforceStorageQuota(String userEmail, long additionalBytes) {
        Subscription sub = subRepo.findForUpdate(userEmail)
                .orElseGet(() -> createFree(userEmail));
        long limit = sub.getPlanLimitBytes();
        if (sub.getUsedBytes() + additionalBytes > limit) {
            throw new ApiException(
                    "Storage quota exceeded for the " + sub.getPlan() + " plan. Upgrade to upload more files.",
                    HttpStatus.PAYMENT_REQUIRED);
        }
        sub.reserve(additionalBytes);
        subRepo.save(sub);
    }

    /**
     * Two-phase direct-upload path — acquires a pessimistic row lock and reserves
     * the declared byte count atomically.
     *
     * <p>The lock prevents concurrent uploads from reading the same {@code usedBytes}
     * value (TOCTOU race). The reservation is committed with the caller's transaction.</p>
     */
    @Transactional
    public void reserveQuota(String userEmail, long declaredBytes) {
        Subscription sub = subRepo.findForUpdate(userEmail)
                .orElseGet(() -> createFree(userEmail));
        long limit = sub.getPlanLimitBytes();
        if (sub.getUsedBytes() + declaredBytes > limit) {
            throw new ApiException(
                    "Storage quota exceeded for the " + sub.getPlan() + " plan. Upgrade to upload more files.",
                    HttpStatus.PAYMENT_REQUIRED);
        }
        sub.reserve(declaredBytes);
        subRepo.save(sub);
    }

    /**
     * Releases previously-reserved bytes when a file is permanently deleted.
     * Acquires a pessimistic lock to prevent concurrent modification.
     */
    @Transactional
    public void releaseQuota(String userEmail, long bytes) {
        Subscription sub = subRepo.findForUpdate(userEmail)
                .orElseThrow(() -> new ApiException("Subscription not found", HttpStatus.NOT_FOUND));
        sub.setUsedBytes(Math.max(0, sub.getUsedBytes() - bytes));
        subRepo.save(sub);
    }

    public long getStorageLimitBytes(String userEmail) {
        return subRepo.findByUserEmail(userEmail)
                .map(Subscription::getPlanLimitBytes)
                .orElse(Subscription.FREE_BYTES);
    }

    // ── periodic reconciliation ─────────────────────────────────────────────

    /**
     * Correctness repair: recomputes {@code usedBytes} from {@code SUM(size)}
     * to heal any drift caused by failed commits or bugs.
     * Runs every 6 hours.
     */
    @Scheduled(fixedDelay = 6 * 60 * 60 * 1000, initialDelay = 60_000)
    @Transactional
    public void reconcileUsedBytes() {
        List<Subscription> all = subRepo.findAll();
        int corrected = 0;
        for (Subscription sub : all) {
            Long actual = fileRepo.sumSizeByUser(sub.getUserEmail());
            long actualBytes = actual != null ? actual : 0L;
            if (sub.getUsedBytes() != actualBytes) {
                log.warn("Quota drift detected for {}: counter={} actual={}",
                        sub.getUserEmail(), sub.getUsedBytes(), actualBytes);
                sub.setUsedBytes(actualBytes);
                subRepo.save(sub);
                corrected++;
            }
        }
        if (corrected > 0) {
            log.info("Quota reconciliation: corrected {} subscription(s)", corrected);
        }
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

    private SubscriptionResponse toResponse(Subscription sub, String userEmail) {
        long used  = sub.getUsedBytes();
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