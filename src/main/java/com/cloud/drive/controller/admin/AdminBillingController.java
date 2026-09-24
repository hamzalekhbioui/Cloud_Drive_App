package com.cloud.drive.controller.admin;

import com.cloud.drive.dto.admin.billing.*;
import com.cloud.drive.service.admin.AdminBillingService;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import com.cloud.drive.security.admin.AdminPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import com.cloud.drive.util.AdminPaging;
import java.util.Set;

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
        return billingService.listSubscriptions(status, plan, startOf(fromDate), endOf(toDate),
                AdminPaging.bounded(pageable, Set.of("createdAt", "userEmail", "status", "plan"), "createdAt"));
    }

    @GetMapping("/payments")
    public Page<AdminPaymentDto> payments(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String plan,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            Pageable pageable) {
        return billingService.listPayments(status, plan, startOf(fromDate), endOf(toDate),
                AdminPaging.bounded(pageable, Set.of("createdAt", "userEmail", "status", "amount"), "createdAt"));
    }

    @GetMapping("/usage")
    public Page<AdminUsageDto> usage(
            @RequestParam(required = false) String userEmail,
            @RequestParam(required = false) String plan,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            Pageable pageable) {
        return billingService.listUsage(userEmail, plan, fromDate, toDate,
                AdminPaging.bounded(pageable, Set.of("periodStart", "periodEnd", "userEmail", "usedBytes"), "periodStart"));
    }

    @PostMapping("/subscriptions/{userId}/plan")
    public AdminSubscriptionDto overridePlan(@PathVariable String userId,
                                             @Valid @RequestBody AdminPlanOverrideRequest request,
                                             @AuthenticationPrincipal AdminPrincipal admin,
                                             HttpServletRequest httpRequest) {
        return billingService.overridePlan(userId, request.getPlanId(), admin, httpRequest.getRemoteAddr());
    }

    @PostMapping("/subscriptions/{userId}/extend")
    public AdminSubscriptionDto extend(@PathVariable String userId,
                                       @Valid @RequestBody AdminExtendSubscriptionRequest request,
                                       @AuthenticationPrincipal AdminPrincipal admin,
                                       HttpServletRequest httpRequest) {
        return billingService.extendSubscription(userId, request.getDays(), admin, httpRequest.getRemoteAddr());
    }

    @PostMapping("/subscriptions/{userId}/cancel")
    public AdminSubscriptionDto cancel(@PathVariable String userId, @AuthenticationPrincipal AdminPrincipal admin,
                                       HttpServletRequest httpRequest) {
        return billingService.cancelSubscription(userId, admin, httpRequest.getRemoteAddr());
    }

    @PostMapping("/usage/{userEmail}/reset")
    public void resetUsage(@PathVariable String userEmail, @AuthenticationPrincipal AdminPrincipal admin,
                           HttpServletRequest httpRequest) {
        billingService.resetUsage(userEmail, admin, httpRequest.getRemoteAddr());
    }

    private static LocalDateTime startOf(LocalDate date) {
        return date == null ? null : date.atStartOfDay();
    }

    private static LocalDateTime endOf(LocalDate date) {
        return date == null ? null : date.plusDays(1).atStartOfDay().minusNanos(1);
    }
}
