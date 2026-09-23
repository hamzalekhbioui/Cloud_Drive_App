package com.cloud.drive.dto.admin.billing;

import java.time.LocalDate;

public class AdminUsageDto {
    private Long id;
    private String userEmail;
    private String resourceType;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private int usageCount;

    public Long getId() { return id; }
    public void setId(Long value) { this.id = value; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String value) { this.userEmail = value; }
    public String getResourceType() { return resourceType; }
    public void setResourceType(String value) { this.resourceType = value; }
    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate value) { this.periodStart = value; }
    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate value) { this.periodEnd = value; }
    public int getUsageCount() { return usageCount; }
    public void setUsageCount(int value) { this.usageCount = value; }
}
