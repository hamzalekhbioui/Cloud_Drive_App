package com.cloud.drive.service;

import com.cloud.drive.dto.settings.DeleteAccountRequest;
import com.cloud.drive.exception.ApiException;
import com.cloud.drive.model.User;
import com.cloud.drive.repository.FileRepository;
import com.cloud.drive.repository.UserRepository;
import com.cloud.drive.repository.UserSettingsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettingsServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserSettingsRepository userSettingsRepository;
    @Mock private FileRepository fileRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private SubscriptionService subscriptionService;

    @InjectMocks private SettingsService settingsService;

    @Test
    void deleteAccount_marksUserDeletedAndRevokesExistingTokens() {
        User user = new User();
        user.setEmail("alice@example.com");
        user.setPassword("encoded");
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "encoded")).thenReturn(true);

        DeleteAccountRequest request = new DeleteAccountRequest();
        request.setConfirmation(true);
        request.setCurrentPassword("secret");

        settingsService.deleteAccount(user.getEmail(), request);

        assertThat(user.getStatus()).isEqualTo(User.STATUS_DELETED);
        assertThat(user.getTokensValidFrom()).isNotNull();
        verify(userRepository).save(user);
    }

    @Test
    void deleteAccount_rejectsWrongPassword() {
        User user = new User();
        user.setEmail("alice@example.com");
        user.setPassword("encoded");
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        DeleteAccountRequest request = new DeleteAccountRequest();
        request.setConfirmation(true);
        request.setCurrentPassword("wrong");

        assertThatThrownBy(() -> settingsService.deleteAccount(user.getEmail(), request))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);

        verify(userRepository, never()).save(any(User.class));
    }
}
