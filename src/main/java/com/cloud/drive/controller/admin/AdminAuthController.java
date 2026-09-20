package com.cloud.drive.controller.admin;

import com.cloud.drive.dto.AuthResponse;
import com.cloud.drive.dto.LoginRequest;
import com.cloud.drive.model.AdminUser;
import com.cloud.drive.security.admin.AdminPrincipal;
import com.cloud.drive.service.admin.AdminAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    public AdminAuthController(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletRequest httpRequest) {
        return ResponseEntity.ok(adminAuthService.login(request, httpRequest.getRemoteAddr()));
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(@AuthenticationPrincipal AdminPrincipal principal) {
        AdminUser admin = adminAuthService.me(principal.getEmail());
        return ResponseEntity.ok(Map.of(
                "id", admin.getId(),
                "email", admin.getEmail(),
                "name", admin.getName(),
                "status", admin.getStatus()));
    }
}
