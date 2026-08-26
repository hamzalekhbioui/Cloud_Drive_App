package com.cloud.drive.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "file_ai_processing")
public class FileAiProcessing {
    @Id
    @Column(name = "file_id")
    private Long fileId;
    @Column(nullable = false, length = 32)
    private String status = "PENDING";
    @Column(columnDefinition = "TEXT")
    private String error;
    @Column(columnDefinition = "TEXT")
    private String summary;
    @Column(name = "processed_at")
    private LocalDateTime processedAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public FileAiProcessing() {}
    public FileAiProcessing(Long fileId) { this.fileId = fileId; this.updatedAt = LocalDateTime.now(); }
    public Long getFileId() { return fileId; }
    public void setFileId(Long fileId) { this.fileId = fileId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
