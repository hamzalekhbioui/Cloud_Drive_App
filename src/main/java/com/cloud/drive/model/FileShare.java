package com.cloud.drive.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "file_shares")
public class FileShare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long fileId;

    @Column(nullable = false)
    private String ownerEmail;

    /** Null = public link (anyone with the token can access). */
    private String sharedWithEmail;

    @Column(nullable = false, unique = true)
    private String token;

    /** VIEW or DOWNLOAD */
    @Column(nullable = false)
    private String permission = "VIEW";

    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    public FileShare() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getFileId() { return fileId; }
    public void setFileId(Long fileId) { this.fileId = fileId; }

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
}