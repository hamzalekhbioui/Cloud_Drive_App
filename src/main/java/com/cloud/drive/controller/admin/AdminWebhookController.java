package com.cloud.drive.controller.admin;

import com.cloud.drive.dto.admin.billing.AdminWebhookEventDto;
import com.cloud.drive.service.admin.AdminBillingService;
import org.springframework.data.domain.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import com.cloud.drive.security.admin.AdminPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/admin/webhooks")
public class AdminWebhookController {
    private final AdminBillingService billingService;

    public AdminWebhookController(AdminBillingService billingService) {
        this.billingService = billingService;
    }

    @GetMapping
    public Page<AdminWebhookEventDto> list(
            @RequestParam(required = false) Boolean processed,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            Pageable pageable) {
        return billingService.listWebhookEvents(processed, startOf(fromDate), endOf(toDate), pageable);
    }

    @GetMapping("/{eventId}")
    public AdminWebhookEventDto detail(@PathVariable Long eventId) {
        return billingService.getWebhookEvent(eventId);
    }

    @PostMapping("/{eventId}/replay")
    public AdminWebhookEventDto replay(@PathVariable Long eventId, @AuthenticationPrincipal AdminPrincipal admin,
                                       HttpServletRequest request) {
        return billingService.replayWebhook(eventId, admin, request.getRemoteAddr());
    }

    private static LocalDateTime startOf(LocalDate date) {
        return date == null ? null : date.atStartOfDay();
    }

    private static LocalDateTime endOf(LocalDate date) {
        return date == null ? null : date.plusDays(1).atStartOfDay().minusNanos(1);
    }
}
