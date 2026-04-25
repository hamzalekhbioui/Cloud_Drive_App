package com.cloud.drive.dto;

import java.time.LocalDateTime;

public class FileResponseDto {
    private Long id;
    private String originalFileName;
    private String url;
    private Long size;
    private String type;
    private LocalDateTime createdAt;
    private boolean starred;
    private LocalDateTime deletedAt;

    public FileResponseDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOriginalFileName() { return originalFileName; }
    public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public Long getSize() { return size; }
    public void setSize(Long size) { this.size = size; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isStarred() { return starred; }
    public void setStarred(boolean starred) { this.starred = starred; }

    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
}
