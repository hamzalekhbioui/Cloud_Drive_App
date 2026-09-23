package com.cloud.drive.controller.admin;

import com.cloud.drive.dto.admin.billing.*;
import com.cloud.drive.service.admin.AdminBillingService;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin/billing")
public class AdminBillingController {
    private final AdminBillingService billingService;

    public AdminBillingController(AdminBillingService billingService) {
        this.billingService = billingService;
    }

    @GetMapping("/subscriptions")
    public Page<AdminSubscriptionDto> subscriptions(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String plan,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            Pageable pageable) {
        return billingService.listSubscriptions(status, plan, startOf(fromDate), endOf(toDate), pageable);
    }

    @GetMapping("/payments")
    public Page<AdminPaymentDto> payments(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String plan,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            Pageable pageable) {
        return billingService.listPayments(status, plan, startOf(fromDate), endOf(toDate), pageable);
    }

    @GetMapping("/usage")
    public Page<AdminUsageDto> usage(
            @RequestParam(required = false) String userEmail,
            @RequestParam(required = false) String plan,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            Pageable pageable) {
        return billingService.listUsage(userEmail, plan, fromDate, toDate, pageable);
    }

    @PostMapping("/subscriptions/{userId}/plan")
    public AdminSubscriptionDto overridePlan(@PathVariable String userId,
                                             @Valid @RequestBody AdminPlanOverrideRequest request) {
        return billingService.overridePlan(userId, request.getPlanId());
    }

    @PostMapping("/subscriptions/{userId}/extend")
    public AdminSubscriptionDto extend(@PathVariable String userId,
                                       @Valid @RequestBody AdminExtendSubscriptionRequest request) {
        return billingService.extendSubscription(userId, request.getDays());
    }

    @PostMapping("/subscriptions/{userId}/cancel")
    public AdminSubscriptionDto cancel(@PathVariable String userId) {
        return billingService.cancelSubscription(userId);
    }

    @PostMapping("/usage/{userEmail}/reset")
    public void resetUsage(@PathVariable String userEmail) {
        billingService.resetUsage(userEmail);
    }

    private static LocalDateTime startOf(LocalDate date) {
        return date == null ? null : date.atStartOfDay();
    }

    private static LocalDateTime endOf(LocalDate date) {
        return date == null ? null : date.plusDays(1).atStartOfDay().minusNanos(1);
    }
}
