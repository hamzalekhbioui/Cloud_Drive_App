package com.cloud.drive.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public class UploadStartRequest {

    @Positive(message = "File size must be greater than zero")
    private long size;

    @NotBlank(message = "File name is required")
    private String rawFileName;

    private Long teamId;

    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }

    public String getRawFileName() { return rawFileName; }
    public void setRawFileName(String rawFileName) { this.rawFileName = rawFileName; }

    public Long getTeamId() { return teamId; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }
}
