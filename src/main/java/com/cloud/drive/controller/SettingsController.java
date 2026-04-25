package com.cloud.drive.controller;

import com.cloud.drive.dto.settings.*;
import com.cloud.drive.service.SettingsService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    /** GET /api/settings — full settings payload (profile + prefs + storage). */
    @GetMapping
    public ResponseEntity<SettingsResponse> getSettings(
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(settingsService.getSettings(user.getUsername()));
    }

    /** PUT /api/settings/preferences — update any preference fields. */
    @PutMapping("/preferences")
    public ResponseEntity<Void> updatePreferences(
            @RequestBody PreferencesUpdateRequest req,
            @AuthenticationPrincipal UserDetails user) {
        settingsService.updatePreferences(user.getUsername(), req);
        return ResponseEntity.noContent().build();
    }

    /** PUT /api/settings/profile — update display name. */
    @PutMapping("/profile")
    public ResponseEntity<Void> updateProfile(
            @Valid @RequestBody ProfileUpdateRequest req,
            @AuthenticationPrincipal UserDetails user) {
        settingsService.updateProfile(user.getUsername(), req);
        return ResponseEntity.noContent().build();
    }

    /** PUT /api/settings/password — change password (validates current password). */
    @PutMapping("/password")
    public ResponseEntity<Void> updatePassword(
            @Valid @RequestBody PasswordUpdateRequest req,
            @AuthenticationPrincipal UserDetails user) {
        settingsService.updatePassword(user.getUsername(), req);
        return ResponseEntity.noContent().build();
    }

    /** POST /api/settings/api-token — regenerate the user's API token. */
    @PostMapping("/api-token")
    public ResponseEntity<Map<String, String>> regenerateToken(
            @AuthenticationPrincipal UserDetails user) {
        String token = settingsService.regenerateApiToken(user.getUsername());
        return ResponseEntity.ok(Map.of("token", token));
    }
}