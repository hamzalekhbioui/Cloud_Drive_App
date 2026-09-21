package com.cloud.drive.dto.admin.overview;

public class StatusCountDto {
    private final String status;
    private final long count;

    public StatusCountDto(String status, long count) {
        this.status = status;
        this.count = count;
    }

    public String getStatus() { return status; }
    public long getCount() { return count; }
}
