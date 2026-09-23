package com.cloud.drive.service.admin;

import com.cloud.drive.dto.admin.billing.*;
import com.cloud.drive.exception.ApiException;
import com.cloud.drive.model.*;
import com.cloud.drive.repository.*;
import com.cloud.drive.service.StripeWebhookService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class AdminBillingService {
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;
    private final UsageTrackingRepository usageRepository;
    private final WebhookEventRepository webhookRepository;
    private final PlanRepository planRepository;
    private final StripeWebhookService stripeWebhookService;

    public AdminBillingService(SubscriptionRepository subscriptionRepository,
                               PaymentRepository paymentRepository,
                               UsageTrackingRepository usageRepository,
                               WebhookEventRepository webhookRepository,
                               PlanRepository planRepository,
                               StripeWebhookService stripeWebhookService) {
        this.subscriptionRepository = subscriptionRepository;
        this.paymentRepository = paymentRepository;
        this.usageRepository = usageRepository;
        this.webhookRepository = webhookRepository;
        this.planRepository = planRepository;
        this.stripeWebhookService = stripeWebhookService;
    }

    @Transactional(readOnly = true)
    public Page<AdminSubscriptionDto> listSubscriptions(String status, String plan,
                                                         LocalDateTime fromDate, LocalDateTime toDate,
                                                         Pageable pageable) {
        return subscriptionRepository.findAllForAdmin(blankToNull(status), blankToNull(plan),
                        fromDate, toDate, pageable).map(this::toSubscriptionDto);
    }

    @Transactional(readOnly = true)
    public Page<AdminPaymentDto> listPayments(String status, String plan, LocalDateTime fromDate,
                                              LocalDateTime toDate, Pageable pageable) {
        return paymentRepository.findAllForAdmin(blankToNull(status), blankToNull(plan), fromDate, toDate, pageable)
                .map(this::toPaymentDto);
    }

    @Transactional(readOnly = true)
    public Page<AdminUsageDto> listUsage(String userEmail, String plan, LocalDate fromDate,
                                         LocalDate toDate, Pageable pageable) {
        return usageRepository.findAllForAdmin(blankToNull(userEmail), blankToNull(plan), fromDate, toDate, pageable)
                .map(this::toUsageDto);
    }

    @Transactional
    public AdminSubscriptionDto overridePlan(String userId, Long planId) {
        Subscription subscription = findSubscriptionForUpdate(userId);
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ApiException("Plan not found", HttpStatus.NOT_FOUND));
        subscription.setPlanRecord(plan);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setCancelAtPeriodEnd(false);
        return toSubscriptionDto(subscriptionRepository.save(subscription));
    }

    @Transactional
    public AdminSubscriptionDto extendSubscription(String userId, int days) {
        if (days < 1) {
            throw new ApiException("Extension must be at least one day", HttpStatus.BAD_REQUEST);
        }
        Subscription subscription = findSubscriptionForUpdate(userId);
        LocalDateTime base = subscription.getCurrentPeriodEnd() != null
                ? subscription.getCurrentPeriodEnd() : LocalDateTime.now();
        LocalDateTime extended = base.plusDays(days);
        subscription.setCurrentPeriodEnd(extended);
        subscription.setEndDate(extended);
        subscription.setCancelAtPeriodEnd(false);
        if (subscription.getStatusValue() == SubscriptionStatus.CANCELLED) {
            subscription.setStatus(SubscriptionStatus.ACTIVE);
        }
        return toSubscriptionDto(subscriptionRepository.save(subscription));
    }

    @Transactional
    public AdminSubscriptionDto cancelSubscription(String userId) {
        Subscription subscription = findSubscriptionForUpdate(userId);
        subscription.cancelNow(LocalDateTime.now());
        return toSubscriptionDto(subscriptionRepository.save(subscription));
    }

    @Transactional
    public void resetUsage(String userEmail) {
        usageRepository.deleteByUserEmail(userEmail);
    }

    @Transactional(readOnly = true)
    public Page<AdminWebhookEventDto> listWebhookEvents(Boolean processed,
                                                        LocalDateTime fromDate, LocalDateTime toDate,
                                                        Pageable pageable) {
        return webhookRepository.findAllForAdmin(processed, fromDate, toDate, pageable)
                .map(this::toWebhookDto);
    }

    @Transactional(readOnly = true)
    public AdminWebhookEventDto getWebhookEvent(Long eventId) {
        return toWebhookDto(webhookRepository.findById(eventId)
                .orElseThrow(() -> new ApiException("Webhook event not found", HttpStatus.NOT_FOUND)));
    }

    @Transactional
    public AdminWebhookEventDto replayWebhook(Long eventId) {
        stripeWebhookService.replayWebhook(eventId);
        return getWebhookEvent(eventId);
    }

    private Subscription findSubscriptionForUpdate(String userId) {
        return subscriptionRepository.findForUpdate(userId)
                .orElseThrow(() -> new ApiException("Subscription not found", HttpStatus.NOT_FOUND));
    }

    private AdminSubscriptionDto toSubscriptionDto(Subscription value) {
        AdminSubscriptionDto dto = new AdminSubscriptionDto();
        dto.setId(value.getId());
        dto.setUserEmail(value.getUserEmail());
        dto.setPlanId(value.getPlanRecord() == null ? null : value.getPlanRecord().getId());
        dto.setPlan(value.getPlan());
        dto.setStatus(value.getStatus());
        dto.setUsedBytes(value.getUsedBytes());
        dto.setStartDate(value.getStartDate());
        dto.setEndDate(value.getEndDate());
        dto.setCurrentPeriodStart(value.getCurrentPeriodStart());
        dto.setCurrentPeriodEnd(value.getCurrentPeriodEnd());
        dto.setCancelAtPeriodEnd(value.isCancelAtPeriodEnd());
        return dto;
    }

    private AdminPaymentDto toPaymentDto(Payment value) {
        AdminPaymentDto dto = new AdminPaymentDto();
        dto.setId(value.getId());
        dto.setUserEmail(value.getUserEmail());
        dto.setSubscriptionId(value.getSubscription() == null ? null : value.getSubscription().getId());
        dto.setStripePaymentIntentId(value.getStripePaymentIntentId());
        dto.setStripeInvoiceId(value.getStripeInvoiceId());
        dto.setAmountCents(value.getAmountCents());
        dto.setCurrency(value.getCurrency());
        dto.setStatus(value.getStatus());
        dto.setCreatedAt(value.getCreatedAt());
        return dto;
    }

    private AdminUsageDto toUsageDto(UsageTracking value) {
        AdminUsageDto dto = new AdminUsageDto();
        dto.setId(value.getId());
        dto.setUserEmail(value.getUserEmail());
        dto.setResourceType(value.getResourceType());
        dto.setPeriodStart(value.getPeriodStart());
        dto.setPeriodEnd(value.getPeriodEnd());
        dto.setUsageCount(value.getUsageCount());
        return dto;
    }

    private AdminWebhookEventDto toWebhookDto(WebhookEvent value) {
        AdminWebhookEventDto dto = new AdminWebhookEventDto();
        dto.setId(value.getId());
        dto.setStripeEventId(value.getStripeEventId());
        dto.setEventType(value.getEventType());
        dto.setPayload(value.getPayload());
        dto.setProcessed(value.isProcessed());
        dto.setProcessingError(value.getProcessingError());
        dto.setCreatedAt(value.getCreatedAt());
        dto.setProcessedAt(value.getProcessedAt());
        return dto;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
