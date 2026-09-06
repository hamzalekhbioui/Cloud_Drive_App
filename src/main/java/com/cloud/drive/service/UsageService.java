package com.cloud.drive.service;

import com.cloud.drive.dto.billing.UsageResponse;
import com.cloud.drive.exception.ApiException;
import com.cloud.drive.model.Plan;
import com.cloud.drive.model.UsageTracking;
import com.cloud.drive.repository.UsageTrackingRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;

@Service
public class UsageService {
    public static final String AI_QUERY = "AI_QUERY";

    private final UsageTrackingRepository usageRepository;
    private final SubscriptionService subscriptionService;
    private final Clock clock;

    @Autowired
    public UsageService(UsageTrackingRepository usageRepository, SubscriptionService subscriptionService) {
        this(usageRepository, subscriptionService, Clock.systemUTC());
    }

    UsageService(UsageTrackingRepository usageRepository, SubscriptionService subscriptionService, Clock clock) {
        this.usageRepository = usageRepository;
        this.subscriptionService = subscriptionService;
        this.clock = clock;
    }

    @Transactional
    public void consumeAiQuery(String userEmail) {
        Plan plan = subscriptionService.getPlanForUser(userEmail);
        LocalDate today = LocalDate.now(clock);
        LocalDate periodStart = today.withDayOfMonth(1);
        LocalDate periodEnd = YearMonth.from(today).atEndOfMonth();
        UsageTracking usage = usageRepository.findForUpdate(userEmail, AI_QUERY, periodStart)
                .orElseGet(() -> newUsage(userEmail, periodStart, periodEnd));
        int limit = plan.getAiQueriesPerMonth();
        if (limit >= 0 && usage.getUsageCount() >= limit) {
            throw new ApiException("Monthly AI query limit reached. Upgrade your plan to continue.",
                    HttpStatus.PAYMENT_REQUIRED);
        }
        usage.setUsageCount(usage.getUsageCount() + 1);
        usageRepository.save(usage);
    }

    @Transactional(readOnly = true)
    public UsageResponse getUsage(String userEmail) {
        Plan plan = subscriptionService.getPlanForUser(userEmail);
        LocalDate today = LocalDate.now(clock);
        LocalDate periodStart = today.withDayOfMonth(1);
        LocalDate periodEnd = YearMonth.from(today).atEndOfMonth();
        int used = usageRepository.findForUpdate(userEmail, AI_QUERY, periodStart)
                .map(UsageTracking::getUsageCount).orElse(0);
        long storageUsed = subscriptionService.getStorageUsedBytes(userEmail);
        long storageLimit = plan.getStorageLimitBytes();
        return new UsageResponse(storageLimit, storageUsed,
                storageLimit > 0 ? storageUsed * 100.0 / storageLimit : 0,
                used, plan.getAiQueriesPerMonth(), periodStart, periodEnd);
    }

    private UsageTracking newUsage(String userEmail, LocalDate periodStart, LocalDate periodEnd) {
        UsageTracking usage = new UsageTracking();
        usage.setUserEmail(userEmail);
        usage.setResourceType(AI_QUERY);
        usage.setPeriodStart(periodStart);
        usage.setPeriodEnd(periodEnd);
        usage.setUsageCount(0);
        return usageRepository.save(usage);
    }
}
