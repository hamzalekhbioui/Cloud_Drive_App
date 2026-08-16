package com.cloud.drive.dto.share;

import java.time.LocalDateTime;

/**
 * Minimal DTO returned to recipients in the "shared-with-me" list.
 * Intentionally omits the share token to prevent recipients from
 * gaining unauthenticated public-stream access (bypassing permission checks).
 */
public class SharedFileResponse {

    private Long id;
    private Long fileId;
    private String fileName;
    private String ownerEmail;
    private String permission;
    private Long size;
    private String type;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getFileId() { return fileId; }
    public void setFileId(Long fileId) { this.fileId = fileId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getOwnerEmail() { return ownerEmail; }
    public void setOwnerEmail(String ownerEmail) { this.ownerEmail = ownerEmail; }

    public String getPermission() { return permission; }
    public void setPermission(String permission) { this.permission = permission; }

    public Long getSize() { return size; }
    public void setSize(Long size) { this.size = size; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
}
