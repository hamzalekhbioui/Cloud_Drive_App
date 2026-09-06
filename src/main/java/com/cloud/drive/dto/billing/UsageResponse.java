package com.cloud.drive.dto.billing;

public class UsageResponse {
    private long storageLimitBytes;
    private long storageUsedBytes;
    private double usagePercent;

    public UsageResponse() {}

    public UsageResponse(long storageLimitBytes, long storageUsedBytes, double usagePercent) {
        this.storageLimitBytes = storageLimitBytes;
        this.storageUsedBytes = storageUsedBytes;
        this.usagePercent = usagePercent;
    }

    public long getStorageLimitBytes() { return storageLimitBytes; }
    public long getStorageUsedBytes() { return storageUsedBytes; }
    public double getUsagePercent() { return usagePercent; }
}
