package com.cloud.drive.dto.settings;

/** Single payload returned by GET /api/settings — combines profile, preferences, and storage. */
public class SettingsResponse {

    // ── Profile ──────────────────────────────────────────────────────────────
    private String name;
    private String email;
    private String memberSince;   // ISO date string
    private String lastLogin;     // ISO date string, may be null
    private boolean hasPassword;  // false for OAuth-only accounts

    // ── Storage summary ───────────────────────────────────────────────────────
    private long storageUsed;
    private long storageTotal;
    private double storagePercentage;
    private long totalFiles;

    // ── Preferences ───────────────────────────────────────────────────────────
    private boolean darkMode;
    private String density;
    private boolean emailNotificationsEnabled;
    private boolean storageWarningEnabled;
    private boolean uploadNotifications;
    private boolean deleteNotifications;
    private boolean inAppNotifications;
    private String defaultView;
    private String defaultSort;
    private boolean autoOrganize;
    private boolean debugMode;
    private String apiToken;

    public SettingsResponse() {}

    // ── Getters & setters ─────────────────────────────────────────────────────
    public String getName()                                { return name; }
    public void setName(String name)                      { this.name = name; }

    public String getEmail()                               { return email; }
    public void setEmail(String email)                    { this.email = email; }

    public String getMemberSince()                         { return memberSince; }
    public void setMemberSince(String memberSince)        { this.memberSince = memberSince; }

    public String getLastLogin()                           { return lastLogin; }
    public void setLastLogin(String lastLogin)            { this.lastLogin = lastLogin; }

    public boolean isHasPassword()                         { return hasPassword; }
    public void setHasPassword(boolean hasPassword)       { this.hasPassword = hasPassword; }

    public long getStorageUsed()                           { return storageUsed; }
    public void setStorageUsed(long storageUsed)          { this.storageUsed = storageUsed; }

    public long getStorageTotal()                          { return storageTotal; }
    public void setStorageTotal(long storageTotal)        { this.storageTotal = storageTotal; }

    public double getStoragePercentage()                   { return storagePercentage; }
    public void setStoragePercentage(double v)            { this.storagePercentage = v; }

    public long getTotalFiles()                            { return totalFiles; }
    public void setTotalFiles(long totalFiles)            { this.totalFiles = totalFiles; }

    public boolean isDarkMode()                            { return darkMode; }
    public void setDarkMode(boolean darkMode)             { this.darkMode = darkMode; }

    public String getDensity()                             { return density; }
    public void setDensity(String density)                { this.density = density; }

    public boolean isEmailNotificationsEnabled()           { return emailNotificationsEnabled; }
    public void setEmailNotificationsEnabled(boolean v)   { this.emailNotificationsEnabled = v; }

    public boolean isStorageWarningEnabled()               { return storageWarningEnabled; }
    public void setStorageWarningEnabled(boolean v)       { this.storageWarningEnabled = v; }

    public boolean isUploadNotifications()                 { return uploadNotifications; }
    public void setUploadNotifications(boolean v)         { this.uploadNotifications = v; }

    public boolean isDeleteNotifications()                 { return deleteNotifications; }
    public void setDeleteNotifications(boolean v)         { this.deleteNotifications = v; }

    public boolean isInAppNotifications()                  { return inAppNotifications; }
    public void setInAppNotifications(boolean v)          { this.inAppNotifications = v; }

    public String getDefaultView()                         { return defaultView; }
    public void setDefaultView(String defaultView)        { this.defaultView = defaultView; }

    public String getDefaultSort()                         { return defaultSort; }
    public void setDefaultSort(String defaultSort)        { this.defaultSort = defaultSort; }

    public boolean isAutoOrganize()                        { return autoOrganize; }
    public void setAutoOrganize(boolean autoOrganize)     { this.autoOrganize = autoOrganize; }

    public boolean isDebugMode()                           { return debugMode; }
    public void setDebugMode(boolean debugMode)           { this.debugMode = debugMode; }

    public String getApiToken()                            { return apiToken; }
    public void setApiToken(String apiToken)              { this.apiToken = apiToken; }
}