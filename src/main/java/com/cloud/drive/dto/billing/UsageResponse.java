package com.cloud.drive.dto.billing;

import java.time.LocalDate;

public class UsageResponse {
    private long storageLimitBytes;
    private long storageUsedBytes;
    private double usagePercent;
    private int aiQueriesUsed;
    private int aiQueriesLimit;
    private LocalDate periodStart;
    private LocalDate periodEnd;

    public UsageResponse() {}

    public UsageResponse(long storageLimitBytes, long storageUsedBytes, double usagePercent) {
        this(storageLimitBytes, storageUsedBytes, usagePercent, 0, 0, null, null);
    }

    public UsageResponse(long storageLimitBytes, long storageUsedBytes, double usagePercent,
                         int aiQueriesUsed, int aiQueriesLimit,
                         LocalDate periodStart, LocalDate periodEnd) {
        this.storageLimitBytes = storageLimitBytes;
        this.storageUsedBytes = storageUsedBytes;
        this.usagePercent = usagePercent;
        this.aiQueriesUsed = aiQueriesUsed;
        this.aiQueriesLimit = aiQueriesLimit;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
    }

    public long getStorageLimitBytes() { return storageLimitBytes; }
    public long getStorageUsedBytes() { return storageUsedBytes; }
    public double getUsagePercent() { return usagePercent; }
    public int getAiQueriesUsed() { return aiQueriesUsed; }
    public int getAiQueriesLimit() { return aiQueriesLimit; }
    public LocalDate getPeriodStart() { return periodStart; }
    public LocalDate getPeriodEnd() { return periodEnd; }
}
