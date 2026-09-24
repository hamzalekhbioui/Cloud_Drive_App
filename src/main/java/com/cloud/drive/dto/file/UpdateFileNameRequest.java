package com.cloud.drive.dto.file;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UpdateFileNameRequest {
    @NotBlank
    @Size(max = 512)
    private String name;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
