package com.cloud.drive.dto.admin.file;

import java.time.LocalDateTime;
import java.util.List;

public class AdminFileDto {
    private Long id;
    private String fileName;
    private String ownerEmail;
    private Long teamId;
    private String teamName;
    private String status;
    private String type;
    private Long size;
    private LocalDateTime createdAt;
    private LocalDateTime deletedAt;
    private String blobFileName;
    private String aiStatus;
    private List<AdminShareDto> shares;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getOwnerEmail() { return ownerEmail; }
    public void setOwnerEmail(String ownerEmail) { this.ownerEmail = ownerEmail; }
    public Long getTeamId() { return teamId; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }
    public String getTeamName() { return teamName; }
    public void setTeamName(String teamName) { this.teamName = teamName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Long getSize() { return size; }
    public void setSize(Long size) { this.size = size; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
    public String getBlobFileName() { return blobFileName; }
    public void setBlobFileName(String blobFileName) { this.blobFileName = blobFileName; }
    public String getAiStatus() { return aiStatus; }
    public void setAiStatus(String aiStatus) { this.aiStatus = aiStatus; }
    public List<AdminShareDto> getShares() { return shares; }
    public void setShares(List<AdminShareDto> shares) { this.shares = shares; }
}
