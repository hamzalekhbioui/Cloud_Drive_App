package com.cloud.drive.dto.analytics;

public class OverviewDto {
    private long totalStorageUsed;
    private long totalStorageLimit;
    private double usagePercentage;
    private long remainingStorage;
    private long totalFiles;

    public OverviewDto(long totalStorageUsed, long totalStorageLimit,
                       double usagePercentage, long remainingStorage, long totalFiles) {
        this.totalStorageUsed = totalStorageUsed;
        this.totalStorageLimit = totalStorageLimit;
        this.usagePercentage = usagePercentage;
        this.remainingStorage = remainingStorage;
        this.totalFiles = totalFiles;
    }

    public long getTotalStorageUsed()   { return totalStorageUsed; }
    public long getTotalStorageLimit()  { return totalStorageLimit; }
    public double getUsagePercentage()  { return usagePercentage; }
    public long getRemainingStorage()   { return remainingStorage; }
    public long getTotalFiles()         { return totalFiles; }
}