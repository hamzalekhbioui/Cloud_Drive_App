package com.cloud.drive.service.admin;

import com.cloud.drive.exception.ApiException;
import com.cloud.drive.model.User;
import com.cloud.drive.repository.UserRepository;
import com.cloud.drive.security.admin.AdminPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AdminUserService {
    private final UserRepository userRepository;
    private final AdminAuditService auditService;

    public AdminUserService(UserRepository userRepository, AdminAuditService auditService) {
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Transactional
    public User disable(Long userId, AdminPrincipal admin, String ip) {
        User user = find(userId);
        auditService.record(admin, AdminAuditActions.USER_DISABLE, "USER", String.valueOf(userId),
                "{\"userId\":" + userId + "}", ip);
        user.setStatus(User.STATUS_DISABLED);
        user.setTokensValidFrom(LocalDateTime.now());
        return userRepository.save(user);
    }

    @Transactional
    public void delete(Long userId, AdminPrincipal admin, String ip) {
        User user = find(userId);
        auditService.record(admin, AdminAuditActions.USER_DELETE, "USER", String.valueOf(userId),
                "{\"userId\":" + userId + "}", ip);
        user.setStatus(User.STATUS_DELETED);
        user.setTokensValidFrom(LocalDateTime.now());
        userRepository.save(user);
    }

    private User find(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
    }
}
