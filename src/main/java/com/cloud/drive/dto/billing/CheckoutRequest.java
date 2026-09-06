package com.cloud.drive.dto.billing;

import jakarta.validation.constraints.NotBlank;

public class CheckoutRequest {
    @NotBlank
    private String plan;

    public String getPlan() { return plan; }
    public void setPlan(String plan) { this.plan = plan; }
}
