package com.cloud.drive.dto.settings;

/** Payload for PUT /api/settings/preferences — all fields are optional (null = no change). */
public class PreferencesUpdateRequest {

    private Boolean darkMode;
    private String density;
    private Boolean emailNotificationsEnabled;
    private Boolean storageWarningEnabled;
    private Boolean uploadNotifications;
    private Boolean deleteNotifications;
    private Boolean inAppNotifications;
    private String defaultView;
    private String defaultSort;
    private Boolean autoOrganize;
    private Boolean debugMode;

    public Boolean getDarkMode()                          { return darkMode; }
    public void setDarkMode(Boolean v)                    { this.darkMode = v; }

    public String getDensity()                            { return density; }
    public void setDensity(String v)                      { this.density = v; }

    public Boolean getEmailNotificationsEnabled()         { return emailNotificationsEnabled; }
    public void setEmailNotificationsEnabled(Boolean v)   { this.emailNotificationsEnabled = v; }

    public Boolean getStorageWarningEnabled()             { return storageWarningEnabled; }
    public void setStorageWarningEnabled(Boolean v)       { this.storageWarningEnabled = v; }

    public Boolean getUploadNotifications()               { return uploadNotifications; }
    public void setUploadNotifications(Boolean v)         { this.uploadNotifications = v; }

    public Boolean getDeleteNotifications()               { return deleteNotifications; }
    public void setDeleteNotifications(Boolean v)         { this.deleteNotifications = v; }

    public Boolean getInAppNotifications()                { return inAppNotifications; }
    public void setInAppNotifications(Boolean v)          { this.inAppNotifications = v; }

    public String getDefaultView()                        { return defaultView; }
    public void setDefaultView(String v)                  { this.defaultView = v; }

    public String getDefaultSort()                        { return defaultSort; }
    public void setDefaultSort(String v)                  { this.defaultSort = v; }

    public Boolean getAutoOrganize()                      { return autoOrganize; }
    public void setAutoOrganize(Boolean v)                { this.autoOrganize = v; }

    public Boolean getDebugMode()                         { return debugMode; }
    public void setDebugMode(Boolean v)                   { this.debugMode = v; }
}
