package com.cloud.drive.dto.admin.overview;

public class StoragePointDto {
    private final String category;
    private final long bytes;

    public StoragePointDto(String category, long bytes) {
        this.category = category;
        this.bytes = bytes;
    }

    public String getCategory() { return category; }
    public long getBytes() { return bytes; }
}
