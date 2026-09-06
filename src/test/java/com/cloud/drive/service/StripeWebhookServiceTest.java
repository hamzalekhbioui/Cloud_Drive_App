package com.cloud.drive.service;

import com.cloud.drive.config.StripeProperties;
import com.cloud.drive.exception.ApiException;
import com.cloud.drive.model.Payment;
import com.cloud.drive.model.Plan;
import com.cloud.drive.model.Subscription;
import com.cloud.drive.model.SubscriptionStatus;
import com.cloud.drive.model.WebhookEvent;
import com.cloud.drive.repository.PaymentRepository;
import com.cloud.drive.repository.PlanRepository;
import com.cloud.drive.repository.SubscriptionRepository;
import com.cloud.drive.repository.WebhookEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StripeWebhookServiceTest {
    private static final String SECRET = "whsec_test";
    private static final String EMAIL = "alice@example.com";

    @Mock private WebhookEventRepository eventRepository;
    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private PlanRepository planRepository;
    @Mock private SubscriptionService subscriptionService;
    @Mock private PlatformTransactionManager transactionManager;
    @Mock private TransactionStatus transactionStatus;

    private StripeWebhookService service;
    private Subscription subscription;

    @BeforeEach
    void setUp() {
        StripeProperties properties = new StripeProperties();
        properties.setWebhookSecret(SECRET);
        lenient().when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
        lenient().doNothing().when(transactionManager).commit(transactionStatus);
        lenient().when(eventRepository.save(any(WebhookEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        service = new StripeWebhookService(properties, new ObjectMapper(), eventRepository,
                subscriptionRepository, paymentRepository, planRepository, subscriptionService,
                transactionManager);

        Plan plan = new Plan();
        plan.setSlug("PRO");
        subscription = new Subscription();
        subscription.setUserEmail(EMAIL);
        subscription.setPlanRecord(plan);
        subscription.setStripeCustomerId("cus_test");
        subscription.setStripeSubscriptionId("sub_test");
        subscription.setStatus(SubscriptionStatus.ACTIVE);
    }

    @Test
    void rejectsInvalidSignatureBeforePersistingOrProcessing() {
        assertThatThrownBy(() -> service.receive("{\"id\":\"evt_bad\",\"type\":\"invoice.paid\"}", "t=1,v1=bad"))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);
        verifyNoInteractions(eventRepository, subscriptionRepository, paymentRepository);
    }

    @Test
    void duplicateProcessedEventIsNoOp() {
        String payload = event("evt_duplicate", "invoice.paid",
                "{\"id\":\"in_1\",\"subscription\":\"sub_test\",\"amount_paid\":999,\"currency\":\"usd\"}");
        WebhookEvent existing = new WebhookEvent();
        existing.setStripeEventId("evt_duplicate");
        existing.setProcessed(true);
        when(eventRepository.findForUpdate("evt_duplicate")).thenReturn(Optional.of(existing));

        service.receive(payload, signature(payload));

        verify(subscriptionRepository, never()).save(any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void checkoutCompletionLinksCustomerSubscriptionAndPlan() {
        String payload = event("evt_checkout", "checkout.session.completed",
                "{\"client_reference_id\":\"alice@example.com\",\"subscription\":\"sub_test\","
                        + "\"customer\":\"cus_new\",\"metadata\":{\"target_plan\":\"PRO\"}}");
        Plan plan = subscription.getPlanRecord();
        when(subscriptionRepository.findForUpdate(EMAIL)).thenReturn(Optional.of(subscription));
        when(planRepository.findBySlug("PRO")).thenReturn(Optional.of(plan));

        service.receive(payload, signature(payload));

        assertThat(subscription.getStripeCustomerId()).isEqualTo("cus_new");
        assertThat(subscription.getStripeSubscriptionId()).isEqualTo("sub_test");
        verify(subscriptionRepository).save(subscription);
    }

    @Test
    void invoicePaidRecordsPaymentAndActivatesSubscription() {
        String payload = event("evt_paid", "invoice.paid",
                "{\"id\":\"in_paid\",\"subscription\":\"sub_test\",\"payment_intent\":\"pi_1\","
                        + "\"amount_paid\":999,\"currency\":\"usd\"}");
        when(subscriptionRepository.findForUpdateByStripeSubscriptionId("sub_test"))
                .thenReturn(Optional.of(subscription));

        service.receive(payload, signature(payload));

        assertThat(subscription.getStatusValue()).isEqualTo(SubscriptionStatus.ACTIVE);
        verify(paymentRepository).save(argThat(payment -> payment.getStatus().equals("PAID")
                && payment.getStripeInvoiceId().equals("in_paid")));
    }

    @Test
    void failedInvoiceMarksPastDueAndRecordsFailedPayment() {
        String payload = event("evt_failed", "invoice.payment_failed",
                "{\"id\":\"in_failed\",\"subscription\":\"sub_test\",\"amount_due\":999,\"currency\":\"usd\"}");
        when(subscriptionRepository.findForUpdateByStripeSubscriptionId("sub_test"))
                .thenReturn(Optional.of(subscription));

        service.receive(payload, signature(payload));

        assertThat(subscription.getStatusValue()).isEqualTo(SubscriptionStatus.PAST_DUE);
        verify(paymentRepository).save(argThat(payment -> payment.getStatus().equals("FAILED")));
    }

    @Test
    void subscriptionUpdatedAppliesStripeStateAndPricePlan() {
        String payload = event("evt_updated", "customer.subscription.updated",
                "{\"id\":\"sub_test\",\"status\":\"past_due\",\"current_period_start\":1788220800,"
                        + "\"current_period_end\":1790812800,\"cancel_at_period_end\":true,"
                        + "\"items\":{\"data\":[{\"price\":{\"id\":\"price_pro\"}}]}}");
        when(subscriptionRepository.findForUpdateByStripeSubscriptionId("sub_test"))
                .thenReturn(Optional.of(subscription));
        Plan plan = subscription.getPlanRecord();
        when(planRepository.findByStripePriceId("price_pro")).thenReturn(Optional.of(plan));

        service.receive(payload, signature(payload));

        assertThat(subscription.getStatusValue()).isEqualTo(SubscriptionStatus.PAST_DUE);
        assertThat(subscription.isCancelAtPeriodEnd()).isTrue();
        verify(subscriptionRepository).save(subscription);
    }

    @Test
    void subscriptionDeletedCancelsSubscription() {
        String payload = event("evt_deleted", "customer.subscription.deleted",
                "{\"id\":\"sub_test\",\"ended_at\":1788220800}");
        when(subscriptionRepository.findForUpdateByStripeSubscriptionId("sub_test"))
                .thenReturn(Optional.of(subscription));

        service.receive(payload, signature(payload));

        assertThat(subscription.getStatusValue()).isEqualTo(SubscriptionStatus.CANCELLED);
        assertThat(subscription.isCancelAtPeriodEnd()).isFalse();
        verify(subscriptionRepository).save(subscription);
    }

    private String event(String id, String type, String object) {
        return "{\"id\":\"" + id + "\",\"object\":\"event\",\"type\":\"" + type
                + "\",\"data\":{\"object\":" + object + "}}";
    }

    private String signature(String payload) {
        long timestamp = Instant.now().getEpochSecond();
        String signed = timestamp + "." + payload;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String digest = HexFormat.of().formatHex(mac.doFinal(signed.getBytes(StandardCharsets.UTF_8)));
            return "t=" + timestamp + ",v1=" + digest;
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }
}
