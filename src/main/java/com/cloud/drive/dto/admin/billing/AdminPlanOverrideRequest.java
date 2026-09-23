package com.cloud.drive.dto.admin.billing;

import jakarta.validation.constraints.NotNull;

public class AdminPlanOverrideRequest {
    @NotNull
    private Long planId;

    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }
}
