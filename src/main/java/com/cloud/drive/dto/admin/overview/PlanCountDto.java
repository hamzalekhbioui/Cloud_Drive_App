package com.cloud.drive.dto.admin.overview;

public class PlanCountDto {
    private final String plan;
    private final long count;

    public PlanCountDto(String plan, long count) {
        this.plan = plan;
        this.count = count;
    }

    public String getPlan() { return plan; }
    public long getCount() { return count; }
}
