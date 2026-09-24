package com.cloud.drive.dto.settings;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;

public class DeleteAccountRequest {

    @Size(max = 255)
    private String currentPassword;

    private boolean confirmation;

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    @AssertTrue(message = "Account deletion must be explicitly confirmed")
    public boolean isConfirmation() {
        return confirmation;
    }

    public void setConfirmation(boolean confirmation) {
        this.confirmation = confirmation;
    }
}
