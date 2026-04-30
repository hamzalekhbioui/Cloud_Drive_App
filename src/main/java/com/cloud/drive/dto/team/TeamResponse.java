package com.cloud.drive.dto.team;

import java.time.LocalDateTime;
import java.util.List;

public class TeamResponse {
    private Long id;
    private String name;
    private String ownerEmail;
    private LocalDateTime createdAt;
    private int memberCount;
    private String callerRole;
    private List<TeamMemberResponse> members;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getOwnerEmail() { return ownerEmail; }
    public void setOwnerEmail(String ownerEmail) { this.ownerEmail = ownerEmail; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public int getMemberCount() { return memberCount; }
    public void setMemberCount(int memberCount) { this.memberCount = memberCount; }

    public String getCallerRole() { return callerRole; }
    public void setCallerRole(String callerRole) { this.callerRole = callerRole; }

    public List<TeamMemberResponse> getMembers() { return members; }
    public void setMembers(List<TeamMemberResponse> members) { this.members = members; }
}