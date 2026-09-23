package com.cloud.drive.service.admin;

import com.cloud.drive.dto.admin.audit.AdminAuditLogDto;
import com.cloud.drive.model.AdminAuditLog;
import com.cloud.drive.repository.AdminAuditLogRepository;
import com.cloud.drive.security.admin.AdminPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AdminAuditService {
    private final AdminAuditLogRepository repository;

    public AdminAuditService(AdminAuditLogRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public AdminAuditLog record(AdminPrincipal admin, String action, String targetType,
                                String targetId, String detailJson, String ip) {
        AdminAuditLog audit = new AdminAuditLog();
        if (admin != null) {
            audit.setAdminId(admin.getId());
            audit.setAdminEmail(admin.getEmail());
        } else {
            audit.setAdminEmail("unknown");
        }
        audit.setAction(action);
        audit.setTargetType(targetType);
        audit.setTargetId(targetId);
        audit.setDetail(detailJson);
        audit.setIpAddress(ip);
        audit.setCreatedAt(LocalDateTime.now());
        return repository.save(audit);
    }

    @Transactional(readOnly = true)
    public Page<AdminAuditLogDto> list(String admin, String action, String targetType,
                                       LocalDateTime fromDate, LocalDateTime toDate,
                                       Pageable pageable) {
        return repository.findAllForAdmin(blankToNull(admin), blankToNull(action),
                        blankToNull(targetType), fromDate, toDate, pageable)
                .map(this::toDto);
    }

    private AdminAuditLogDto toDto(AdminAuditLog value) {
        AdminAuditLogDto dto = new AdminAuditLogDto();
        dto.setId(value.getId());
        dto.setAdminId(value.getAdminId());
        dto.setAdminEmail(value.getAdminEmail());
        dto.setAction(value.getAction());
        dto.setTargetType(value.getTargetType());
        dto.setTargetId(value.getTargetId());
        dto.setDetailJson(value.getDetail());
        dto.setIp(value.getIpAddress());
        dto.setCreatedAt(value.getCreatedAt());
        return dto;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
