package com.cloud.drive.dto.folder;

import java.time.LocalDateTime;

public class FolderResponse {
    private Long id;
    private String name;
    private String userId;
    private Long teamId;
    private Long parentId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public FolderResponse() {}
    public FolderResponse(Long id, String name, String userId, Long teamId, Long parentId,
                          LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id; this.name = name; this.userId = userId; this.teamId = teamId;
        this.parentId = parentId; this.createdAt = createdAt; this.updatedAt = updatedAt;
    }
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getUserId() { return userId; }
    public Long getTeamId() { return teamId; }
    public Long getParentId() { return parentId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
