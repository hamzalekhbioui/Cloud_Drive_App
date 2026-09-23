package com.cloud.drive.dto.admin.billing;

import jakarta.validation.constraints.Min;

public class AdminExtendSubscriptionRequest {
    @Min(1)
    private int days;

    public int getDays() { return days; }
    public void setDays(int days) { this.days = days; }
}
