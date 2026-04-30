package com.cloud.drive.dto.share;

import java.time.LocalDateTime;

public class ShareResponse {
    private Long id;
    private Long fileId;
    private String fileName;
    private String ownerEmail;
    private String sharedWithEmail;
    private String token;
    private String permission;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    /** Populated when this is a file-shared-with-me response. */
    private String publicLink;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getFileId() { return fileId; }
    public void setFileId(Long fileId) { this.fileId = fileId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getOwnerEmail() { return ownerEmail; }
    public void setOwnerEmail(String ownerEmail) { this.ownerEmail = ownerEmail; }

    public String getSharedWithEmail() { return sharedWithEmail; }
    public void setSharedWithEmail(String sharedWithEmail) { this.sharedWithEmail = sharedWithEmail; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getPermission() { return permission; }
    public void setPermission(String permission) { this.permission = permission; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public String getPublicLink() { return publicLink; }
    public void setPublicLink(String publicLink) { this.publicLink = publicLink; }
}