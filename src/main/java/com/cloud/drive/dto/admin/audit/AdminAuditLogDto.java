package com.cloud.drive.dto.admin.audit;

import java.time.LocalDateTime;

public class AdminAuditLogDto {
    private Long id;
    private Long adminId;
    private String adminEmail;
    private String action;
    private String targetType;
    private String targetId;
    private String detailJson;
    private String ip;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long value) { this.id = value; }
    public Long getAdminId() { return adminId; }
    public void setAdminId(Long value) { this.adminId = value; }
    public String getAdminEmail() { return adminEmail; }
    public void setAdminEmail(String value) { this.adminEmail = value; }
    public String getAction() { return action; }
    public void setAction(String value) { this.action = value; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String value) { this.targetType = value; }
    public String getTargetId() { return targetId; }
    public void setTargetId(String value) { this.targetId = value; }
    public String getDetailJson() { return detailJson; }
    public void setDetailJson(String value) { this.detailJson = value; }
    public String getIp() { return ip; }
    public void setIp(String value) { this.ip = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime value) { this.createdAt = value; }
}
