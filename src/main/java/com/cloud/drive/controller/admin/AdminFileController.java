package com.cloud.drive.controller.admin;

import com.cloud.drive.dto.admin.file.AdminFileDto;
import com.cloud.drive.service.admin.AdminFileService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.cloud.drive.security.admin.AdminPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import com.cloud.drive.util.AdminPaging;

@RestController
@RequestMapping("/api/admin/files")
public class AdminFileController {
    private final AdminFileService fileService;

    public AdminFileController(AdminFileService fileService) {
        this.fileService = fileService;
    }

    @GetMapping
    public Page<AdminFileDto> list(@RequestParam(required = false) String owner,
                                   @RequestParam(required = false) String status,
                                   @RequestParam(required = false) String type,
                                   @RequestParam(required = false) Long minSize,
                                   @RequestParam(required = false) Long maxSize,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "20") int size,
                                   @RequestParam(defaultValue = "createdAt") String sort,
                                   @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        String safeSort = switch (sort) {
            case "createdAt", "size", "originalFileName", "type", "status" -> sort;
            default -> "createdAt";
        };
        Pageable pageable = AdminPaging.bounded(page, size, Sort.by(direction, safeSort));
        return fileService.listFiles(owner, status, type, minSize, maxSize,
                fromDate == null ? null : fromDate.atStartOfDay(),
                toDate == null ? null : toDate.plusDays(1).atStartOfDay().minusNanos(1), pageable);
    }

    @GetMapping("/{fileId}")
    public AdminFileDto detail(@PathVariable Long fileId) {
        return fileService.getFile(fileId);
    }

    @PostMapping("/{fileId}/delete")
    public AdminFileDto softDelete(@PathVariable Long fileId, @AuthenticationPrincipal AdminPrincipal admin,
                                   HttpServletRequest request) {
        return fileService.softDelete(fileId, admin, request.getRemoteAddr());
    }

    @PostMapping("/{fileId}/restore")
    public AdminFileDto restore(@PathVariable Long fileId, @AuthenticationPrincipal AdminPrincipal admin,
                                HttpServletRequest request) {
        return fileService.restore(fileId, admin, request.getRemoteAddr());
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void> purge(@PathVariable Long fileId, @AuthenticationPrincipal AdminPrincipal admin,
                                      HttpServletRequest request) {
        fileService.purge(fileId, admin, request.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }
}
