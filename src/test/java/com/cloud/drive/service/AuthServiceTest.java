package com.cloud.drive.service;

import com.cloud.drive.dto.AuthResponse;
import com.cloud.drive.dto.LoginRequest;
import com.cloud.drive.dto.RegisterRequest;
import com.cloud.drive.exception.ApiException;
import com.cloud.drive.model.User;
import com.cloud.drive.repository.UserRepository;
import com.cloud.drive.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private AuthenticationManager authenticationManager;

    @InjectMocks private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setEmail("alice@example.com");
        registerRequest.setName("Alice");
        registerRequest.setPassword("password123");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("alice@example.com");
        loginRequest.setPassword("password123");
    }

    @Test
    void register_createsUserAndReturnsToken() {
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(jwtUtil.generateToken("alice@example.com")).thenReturn("jwt-token");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthResponse response = authService.register(registerRequest);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getEmail()).isEqualTo("alice@example.com");
        assertThat(response.getName()).isEqualTo("Alice");
        verify(userRepository).save(argThat(u ->
                u.getEmail().equals("alice@example.com")
                        && u.getPassword().equals("hashed")
                        && u.getCreatedAt() != null));
    }

    @Test
    void register_throwsConflict_whenEmailExists() {
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT);

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_authenticatesAndReturnsToken() {
        User user = new User();
        user.setEmail("alice@example.com");
        user.setName("Alice");

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken("alice@example.com")).thenReturn("jwt-token");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthResponse response = authService.login(loginRequest);

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(user.getLastLogin()).isNotNull();
    }

    @Test
    void login_propagatesAuthenticationFailure() {
        doThrow(new org.springframework.security.authentication.BadCredentialsException("bad creds"))
                .when(authenticationManager).authenticate(any());

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(org.springframework.security.authentication.BadCredentialsException.class);

        verify(userRepository, never()).save(any());
    }
}
