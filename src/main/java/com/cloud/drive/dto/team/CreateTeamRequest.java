package com.cloud.drive.dto.team;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateTeamRequest {

    @NotBlank
    @Size(min = 2, max = 100)
    private String name;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}