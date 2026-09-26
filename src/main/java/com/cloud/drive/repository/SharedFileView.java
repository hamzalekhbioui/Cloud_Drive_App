package com.cloud.drive.repository;

import java.time.LocalDateTime;

/** Read-only recipient inbox projection; avoids loading a share and then its file. */
public interface SharedFileView {
    Long getId();
    Long getFileId();
    String getFileName();
    String getOwnerEmail();
    String getPermission();
    LocalDateTime getCreatedAt();
    LocalDateTime getExpiresAt();
    Long getSize();
    String getType();
}
