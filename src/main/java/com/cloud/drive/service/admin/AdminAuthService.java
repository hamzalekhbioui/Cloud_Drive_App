package com.cloud.drive.service.admin;

import com.cloud.drive.dto.AuthResponse;
import com.cloud.drive.dto.LoginRequest;
import com.cloud.drive.exception.ApiException;
import com.cloud.drive.model.AdminAuditLog;
import com.cloud.drive.model.AdminUser;
import com.cloud.drive.repository.AdminAuditLogRepository;
import com.cloud.drive.repository.AdminUserRepository;
import com.cloud.drive.security.admin.AdminJwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AdminAuthService {

    private final AdminUserRepository adminUserRepository;
    private final AdminAuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminJwtUtil adminJwtUtil;

    public AdminAuthService(AdminUserRepository adminUserRepository,
                            AdminAuditLogRepository auditLogRepository,
                            PasswordEncoder passwordEncoder,
                            AdminJwtUtil adminJwtUtil) {
        this.adminUserRepository = adminUserRepository;
        this.auditLogRepository = auditLogRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminJwtUtil = adminJwtUtil;
    }

    @Transactional
    public AuthResponse login(LoginRequest request, String ipAddress) {
        AdminUser admin = adminUserRepository.findByEmail(request.getEmail())
                .filter(candidate -> AdminUser.STATUS_ACTIVE.equals(candidate.getStatus()))
                .filter(candidate -> passwordEncoder.matches(request.getPassword(), candidate.getPassword()))
                .orElseThrow(() -> new ApiException("Invalid email or password", HttpStatus.UNAUTHORIZED));

        admin.setLastLogin(LocalDateTime.now());
        adminUserRepository.save(admin);

        AdminAuditLog audit = new AdminAuditLog();
        audit.setAdminId(admin.getId());
        audit.setAdminEmail(admin.getEmail());
        audit.setAction("ADMIN_LOGIN");
        audit.setIpAddress(ipAddress);
        audit.setCreatedAt(LocalDateTime.now());
        auditLogRepository.save(audit);

        return new AuthResponse(adminJwtUtil.generateToken(admin.getEmail()), admin.getEmail(), admin.getName());
    }

    @Transactional(readOnly = true)
    public AdminUser me(String email) {
        return adminUserRepository.findByEmail(email)
                .filter(admin -> AdminUser.STATUS_ACTIVE.equals(admin.getStatus()))
                .orElseThrow(() -> new ApiException("Admin not found", HttpStatus.UNAUTHORIZED));
    }
}
