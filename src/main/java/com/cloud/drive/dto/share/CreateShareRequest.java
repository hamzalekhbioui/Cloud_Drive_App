package com.cloud.drive.dto.share;

import jakarta.validation.constraints.Pattern;
import java.time.LocalDateTime;

public class CreateShareRequest {

    /** Null means public link. */
    private String sharedWithEmail;

    @Pattern(regexp = "VIEW|DOWNLOAD")
    private String permission = "VIEW";

    private LocalDateTime expiresAt;

    public String getSharedWithEmail() { return sharedWithEmail; }
    public void setSharedWithEmail(String sharedWithEmail) { this.sharedWithEmail = sharedWithEmail; }

    public String getPermission() { return permission; }
    public void setPermission(String permission) { this.permission = permission; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
}