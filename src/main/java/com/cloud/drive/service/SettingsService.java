package com.cloud.drive.service;

import com.cloud.drive.dto.settings.*;
import com.cloud.drive.exception.ApiException;
import com.cloud.drive.model.User;
import com.cloud.drive.model.UserSettings;
import com.cloud.drive.repository.FileRepository;
import com.cloud.drive.repository.UserRepository;
import com.cloud.drive.repository.UserSettingsRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class SettingsService {

    private static final long STORAGE_LIMIT = 1024L * 1024 * 1024 * 1024; // 1 TB

    private final UserRepository           userRepository;
    private final UserSettingsRepository   userSettingsRepository;
    private final FileRepository           fileRepository;
    private final PasswordEncoder          passwordEncoder;

    public SettingsService(UserRepository userRepository,
                           UserSettingsRepository userSettingsRepository,
                           FileRepository fileRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository         = userRepository;
        this.userSettingsRepository = userSettingsRepository;
        this.fileRepository         = fileRepository;
        this.passwordEncoder        = passwordEncoder;
    }

    // ── GET /api/settings ────────────────────────────────────────────────────

    public SettingsResponse getSettings(String userId) {
        User         user = findUser(userId);
        UserSettings prefs = getOrCreateSettings(userId);

        long used  = fileRepository.sumSizeByUser(userId);
        long files = fileRepository.countByUserIdAndDeletedAtIsNull(userId);
        double pct = STORAGE_LIMIT > 0 ? (used * 100.0) / STORAGE_LIMIT : 0;

        SettingsResponse r = new SettingsResponse();

        // Profile
        r.setName(user.getName());
        r.setEmail(user.getEmail());
        r.setMemberSince(user.getCreatedAt() != null ? user.getCreatedAt().toString() : null);
        r.setLastLogin(user.getLastLogin() != null ? user.getLastLogin().toString() : null);
        r.setHasPassword(user.getPassword() != null);

        // Storage
        r.setStorageUsed(used);
        r.setStorageTotal(STORAGE_LIMIT);
        r.setStoragePercentage(pct);
        r.setTotalFiles(files);

        // Preferences
        r.setDarkMode(prefs.isDarkMode());
        r.setDensity(prefs.getDensity());
        r.setEmailNotificationsEnabled(prefs.isEmailNotificationsEnabled());
        r.setStorageWarningEnabled(prefs.isStorageWarningEnabled());
        r.setUploadNotifications(prefs.isUploadNotifications());
        r.setDeleteNotifications(prefs.isDeleteNotifications());
        r.setInAppNotifications(prefs.isInAppNotifications());
        r.setDefaultView(prefs.getDefaultView());
        r.setDefaultSort(prefs.getDefaultSort());
        r.setAutoOrganize(prefs.isAutoOrganize());
        r.setDebugMode(prefs.isDebugMode());
        r.setApiToken(prefs.getApiToken());

        return r;
    }

    // ── PUT /api/settings/preferences ────────────────────────────────────────

    @Transactional
    public void updatePreferences(String userId, PreferencesUpdateRequest req) {
        UserSettings s = getOrCreateSettings(userId);

        // Apply only fields that were sent (non-null)
        if (req.getDarkMode()                  != null) s.setDarkMode(req.getDarkMode());
        if (req.getDensity()                   != null) s.setDensity(req.getDensity());
        if (req.getEmailNotificationsEnabled() != null) s.setEmailNotificationsEnabled(req.getEmailNotificationsEnabled());
        if (req.getStorageWarningEnabled()     != null) s.setStorageWarningEnabled(req.getStorageWarningEnabled());
        if (req.getUploadNotifications()       != null) s.setUploadNotifications(req.getUploadNotifications());
        if (req.getDeleteNotifications()       != null) s.setDeleteNotifications(req.getDeleteNotifications());
        if (req.getInAppNotifications()        != null) s.setInAppNotifications(req.getInAppNotifications());
        if (req.getDefaultView()               != null) s.setDefaultView(req.getDefaultView());
        if (req.getDefaultSort()               != null) s.setDefaultSort(req.getDefaultSort());
        if (req.getAutoOrganize()              != null) s.setAutoOrganize(req.getAutoOrganize());
        if (req.getDebugMode()                 != null) s.setDebugMode(req.getDebugMode());

        s.setUpdatedAt(LocalDateTime.now());
        userSettingsRepository.save(s);
    }

    // ── PUT /api/settings/profile ────────────────────────────────────────────

    @Transactional
    public void updateProfile(String userId, ProfileUpdateRequest req) {
        User user = findUser(userId);
        user.setName(req.getName().trim());
        userRepository.save(user);
    }

    // ── PUT /api/settings/password ───────────────────────────────────────────

    @Transactional
    public void updatePassword(String userId, PasswordUpdateRequest req) {
        User user = findUser(userId);

        // If the account already has a password, verify the current one
        if (user.getPassword() != null) {
            if (req.getCurrentPassword() == null || req.getCurrentPassword().isBlank()) {
                throw new ApiException("Current password is required", HttpStatus.BAD_REQUEST);
            }
            if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPassword())) {
                throw new ApiException("Current password is incorrect", HttpStatus.BAD_REQUEST);
            }
        }

        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);
    }

    // ── POST /api/settings/api-token ─────────────────────────────────────────

    @Transactional
    public String regenerateApiToken(String userId) {
        UserSettings s = getOrCreateSettings(userId);
        String token = UUID.randomUUID().toString();
        s.setApiToken(token);
        s.setUpdatedAt(LocalDateTime.now());
        userSettingsRepository.save(s);
        return token;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private User findUser(String userId) {
        return userRepository.findByEmail(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
    }

    /** Returns existing settings or creates defaults — never returns null. */
    private UserSettings getOrCreateSettings(String userId) {
        return userSettingsRepository.findByUserId(userId)
                .orElseGet(() -> userSettingsRepository.save(UserSettings.defaults(userId)));
    }
}