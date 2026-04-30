package com.cloud.drive.dto.team;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class InviteMemberRequest {

    @NotBlank
    @Email
    private String email;

    @Pattern(regexp = "ADMIN|MEMBER")
    private String role = "MEMBER";

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}