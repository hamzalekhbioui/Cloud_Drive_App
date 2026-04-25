package com.cloud.drive.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_settings")
public class UserSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Matches the email used as userId throughout the app. */
    @Column(unique = true, nullable = false)
    private String userId;

    // ── Appearance ──────────────────────────────────────────────────────────
    private boolean darkMode = false;
    private String density = "comfortable";   // compact | comfortable | airy

    // ── Notifications ───────────────────────────────────────────────────────
    private boolean emailNotificationsEnabled = true;
    private boolean storageWarningEnabled = true;
    private boolean uploadNotifications = false;
    private boolean deleteNotifications = false;
    private boolean inAppNotifications = true;

    // ── File preferences ────────────────────────────────────────────────────
    private String defaultView = "grid";      // grid | list
    private String defaultSort = "date";      // name | size | date
    private boolean autoOrganize = false;

    // ── Advanced ────────────────────────────────────────────────────────────
    private boolean debugMode = false;

    @Column(length = 64)
    private String apiToken;

    private LocalDateTime updatedAt;

    public UserSettings() {}

    /** Factory: create default settings for a new user. */
    public static UserSettings defaults(String userId) {
        UserSettings s = new UserSettings();
        s.userId = userId;
        s.apiToken = UUID.randomUUID().toString();
        s.updatedAt = LocalDateTime.now();
        return s;
    }

    // ── Getters & setters ────────────────────────────────────────────────────
    public Long getId()                               { return id; }
    public String getUserId()                         { return userId; }
    public void setUserId(String userId)              { this.userId = userId; }

    public boolean isDarkMode()                       { return darkMode; }
    public void setDarkMode(boolean darkMode)         { this.darkMode = darkMode; }

    public String getDensity()                        { return density; }
    public void setDensity(String density)            { this.density = density; }

    public boolean isEmailNotificationsEnabled()      { return emailNotificationsEnabled; }
    public void setEmailNotificationsEnabled(boolean v){ this.emailNotificationsEnabled = v; }

    public boolean isStorageWarningEnabled()          { return storageWarningEnabled; }
    public void setStorageWarningEnabled(boolean v)   { this.storageWarningEnabled = v; }

    public boolean isUploadNotifications()            { return uploadNotifications; }
    public void setUploadNotifications(boolean v)     { this.uploadNotifications = v; }

    public boolean isDeleteNotifications()            { return deleteNotifications; }
    public void setDeleteNotifications(boolean v)     { this.deleteNotifications = v; }

    public boolean isInAppNotifications()             { return inAppNotifications; }
    public void setInAppNotifications(boolean v)      { this.inAppNotifications = v; }

    public String getDefaultView()                    { return defaultView; }
    public void setDefaultView(String defaultView)    { this.defaultView = defaultView; }

    public String getDefaultSort()                    { return defaultSort; }
    public void setDefaultSort(String defaultSort)    { this.defaultSort = defaultSort; }

    public boolean isAutoOrganize()                   { return autoOrganize; }
    public void setAutoOrganize(boolean autoOrganize) { this.autoOrganize = autoOrganize; }

    public boolean isDebugMode()                      { return debugMode; }
    public void setDebugMode(boolean debugMode)       { this.debugMode = debugMode; }

    public String getApiToken()                       { return apiToken; }
    public void setApiToken(String apiToken)          { this.apiToken = apiToken; }

    public LocalDateTime getUpdatedAt()               { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
