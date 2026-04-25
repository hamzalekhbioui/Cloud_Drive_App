package com.cloud.drive.dto.analytics;

public class ActivityItemDto {
    private String date;
    private long totalUploadedSize;
    private int fileCount;

    public ActivityItemDto(String date, long totalUploadedSize, int fileCount) {
        this.date = date;
        this.totalUploadedSize = totalUploadedSize;
        this.fileCount = fileCount;
    }

    public String getDate()              { return date; }
    public long getTotalUploadedSize()   { return totalUploadedSize; }
    public int getFileCount()            { return fileCount; }
}