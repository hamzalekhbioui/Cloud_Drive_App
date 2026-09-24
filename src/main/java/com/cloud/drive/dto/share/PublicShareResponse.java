package com.cloud.drive.dto.share;

import java.time.LocalDateTime;

public class PublicShareResponse {
    private Long id;
    private String originalFileName;
    private Long size;
    private String type;
    private LocalDateTime createdAt;
    private String permission;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOriginalFileName() { return originalFileName; }
    public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }

    public Long getSize() { return size; }
    public void setSize(Long size) { this.size = size; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getPermission() { return permission; }
    public void setPermission(String permission) { this.permission = permission; }
}
