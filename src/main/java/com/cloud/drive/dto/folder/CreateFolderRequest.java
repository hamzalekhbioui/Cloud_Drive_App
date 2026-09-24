package com.cloud.drive.dto.folder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateFolderRequest {
    @NotBlank
    @Size(max = 255)
    private String name;
    private Long teamId;
    private Long parentId;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getTeamId() { return teamId; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
}
