package com.cloud.drive.dto.admin.overview;

public class GrowthPointDto {
    private final String date;
    private final long signups;
    private final long uploads;
    private final long uploadedBytes;

    public GrowthPointDto(String date, long signups, long uploads, long uploadedBytes) {
        this.date = date;
        this.signups = signups;
        this.uploads = uploads;
        this.uploadedBytes = uploadedBytes;
    }

    public String getDate() { return date; }
    public long getSignups() { return signups; }
    public long getUploads() { return uploads; }
    public long getUploadedBytes() { return uploadedBytes; }
}
