package com.cloud.drive.dto.settings;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PasswordUpdateRequest {

    /** May be empty/null for OAuth accounts that are setting a password for the first time. */
    private String currentPassword;

    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String newPassword;

    public String getCurrentPassword()            { return currentPassword; }
    public void setCurrentPassword(String p)      { this.currentPassword = p; }

    public String getNewPassword()                { return newPassword; }
    public void setNewPassword(String p)          { this.newPassword = p; }
}