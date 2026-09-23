package com.cloud.drive.controller.admin;

import com.cloud.drive.dto.admin.audit.AdminAuditLogDto;
import com.cloud.drive.service.admin.AdminAuditService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin/audit")
public class AdminAuditController {
    private final AdminAuditService auditService;

    public AdminAuditController(AdminAuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public Page<AdminAuditLogDto> list(@RequestParam(required = false) String admin,
                                      @RequestParam(required = false) String action,
                                      @RequestParam(required = false) String targetType,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
                                      Pageable pageable) {
        return auditService.list(admin, action, targetType, startOf(fromDate), endOf(toDate), pageable);
    }

    private static LocalDateTime startOf(LocalDate value) {
        return value == null ? null : value.atStartOfDay();
    }

    private static LocalDateTime endOf(LocalDate value) {
        return value == null ? null : value.plusDays(1).atStartOfDay().minusNanos(1);
    }
}
