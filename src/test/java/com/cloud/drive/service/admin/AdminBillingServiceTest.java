package com.cloud.drive.service.admin;

import com.cloud.drive.dto.admin.billing.AdminSubscriptionDto;
import com.cloud.drive.dto.admin.billing.AdminWebhookEventDto;
import com.cloud.drive.model.Plan;
import com.cloud.drive.model.Subscription;
import com.cloud.drive.model.WebhookEvent;
import com.cloud.drive.repository.*;
import com.cloud.drive.service.StripeWebhookService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminBillingServiceTest {
    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private UsageTrackingRepository usageRepository;
    @Mock private WebhookEventRepository webhookRepository;
    @Mock private PlanRepository planRepository;
    @Mock private StripeWebhookService stripeWebhookService;

    @InjectMocks private AdminBillingService service;

    @Test
    void overridePlan_updatesSubscriptionPlanImmediately() {
        Plan plan = new Plan();
        plan.setId(2L);
        plan.setSlug("PRO");
        plan.setStorageLimitBytes(10_000L);
        Subscription subscription = new Subscription();
        subscription.setId(8L);
        subscription.setUserEmail("owner@example.com");
        subscription.setPlanRecord(plan);

        Plan business = new Plan();
        business.setId(3L);
        business.setSlug("BUSINESS");
        business.setStorageLimitBytes(100_000L);
        when(subscriptionRepository.findForUpdate("owner@example.com")).thenReturn(Optional.of(subscription));
        when(planRepository.findById(3L)).thenReturn(Optional.of(business));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminSubscriptionDto result = service.overridePlan("owner@example.com", 3L);

        assertThat(subscription.getPlan()).isEqualTo("BUSINESS");
        assertThat(subscription.getPlanRecord()).isSameAs(business);
        assertThat(result.getPlan()).isEqualTo("BUSINESS");
        verify(subscriptionRepository).save(subscription);
    }

    @Test
    void replayWebhook_delegatesToIdempotentWebhookProcessor() {
        WebhookEvent event = new WebhookEvent();
        event.setStripeEventId("evt_failed");
        event.setEventType("invoice.paid");
        event.setPayload("{}");
        event.setProcessed(false);
        when(webhookRepository.findById(4L)).thenReturn(Optional.of(event));

        service.replayWebhook(4L);

        verify(stripeWebhookService).replayWebhook(4L);
        verify(webhookRepository).findById(4L);
    }
}
