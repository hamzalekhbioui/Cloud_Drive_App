package com.cloud.drive.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "files")
public class FileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 512)
    private String originalFileName;
    @Column(length = 512)
    private String blobFileName;
    @Column(columnDefinition = "TEXT")
    private String url;
    private Long size;
    private String type;
    private String userId;
    private LocalDateTime createdAt;
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean starred = false;
    private LocalDateTime deletedAt;

    public FileEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOriginalFileName() { return originalFileName; }
    public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }

    public String getBlobFileName() { return blobFileName; }
    public void setBlobFileName(String blobFileName) { this.blobFileName = blobFileName; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public Long getSize() { return size; }
    public void setSize(Long size) { this.size = size; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isStarred() { return starred; }
    public void setStarred(boolean starred) { this.starred = starred; }

    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
}
