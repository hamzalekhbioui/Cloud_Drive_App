package com.cloud.drive.dto.subscription;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class ChangePlanRequest {

    @NotBlank
    @Pattern(regexp = "FREE|PRO|BUSINESS")
    private String plan;

    public String getPlan() { return plan; }
    public void setPlan(String plan) { this.plan = plan; }
}