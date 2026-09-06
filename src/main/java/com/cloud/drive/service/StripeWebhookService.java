package com.cloud.drive.service;

import com.cloud.drive.config.StripeProperties;
import com.cloud.drive.exception.ApiException;
import com.cloud.drive.model.*;
import com.cloud.drive.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class StripeWebhookService {
    private static final Logger log = LoggerFactory.getLogger(StripeWebhookService.class);

    private final StripeProperties properties;
    private final ObjectMapper objectMapper;
    private final WebhookEventRepository eventRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;
    private final PlanRepository planRepository;
    private final SubscriptionService subscriptionService;
    private final TransactionTemplate transactionTemplate;

    public StripeWebhookService(StripeProperties properties, ObjectMapper objectMapper,
                                 WebhookEventRepository eventRepository,
                                 SubscriptionRepository subscriptionRepository,
                                 PaymentRepository paymentRepository,
                                 PlanRepository planRepository,
                                 SubscriptionService subscriptionService,
                                 org.springframework.transaction.PlatformTransactionManager transactionManager) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.eventRepository = eventRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.paymentRepository = paymentRepository;
        this.planRepository = planRepository;
        this.subscriptionService = subscriptionService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public void receive(String payload, String signature) {
        if (properties.getWebhookSecret() == null || properties.getWebhookSecret().isBlank()) {
            throw new ApiException("Stripe webhook is not configured", HttpStatus.SERVICE_UNAVAILABLE);
        }
        final Event event;
        try {
            event = Webhook.constructEvent(payload, signature, properties.getWebhookSecret());
        } catch (SignatureVerificationException | IllegalArgumentException e) {
            log.warn("stripe_webhook_signature_rejected errorType={}", e.getClass().getSimpleName());
            throw new ApiException("Invalid Stripe webhook signature", HttpStatus.BAD_REQUEST);
        }

        try {
            transactionTemplate.executeWithoutResult(status -> processInternal(
                    event.getId(), event.getType(), payload));
        } catch (Exception e) {
            recordFailure(event.getId(), event.getType(), payload, e);
            log.error("stripe_webhook_processing_failed eventId={} eventType={} errorType={} message={}",
                    event.getId(), event.getType(), e.getClass().getSimpleName(), e.getMessage(), e);
            throw new ApiException("Webhook processing failed", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void processInternal(String eventId, String eventType, String payload) {
        WebhookEvent event = eventRepository.findForUpdate(eventId).orElseGet(() -> {
            WebhookEvent created = new WebhookEvent();
            created.setStripeEventId(eventId);
            created.setEventType(eventType);
            created.setPayload(payload);
            created.setCreatedAt(LocalDateTime.now());
            return eventRepository.save(created);
        });
        if (event.isProcessed()) {
            log.info("stripe_webhook_duplicate_noop eventId={} eventType={}", eventId, eventType);
            return;
        }

        final JsonNode root;
        try {
            root = objectMapper.readTree(payload);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid webhook JSON", e);
        }
        processEvent(eventType, root.path("data").path("object"));
        event.setProcessed(true);
        event.setProcessingError(null);
        event.setProcessedAt(LocalDateTime.now());
        eventRepository.save(event);
        log.info("stripe_webhook_processed eventId={} eventType={}", eventId, eventType);
    }

    private void recordFailure(String eventId, String eventType, String payload, Exception error) {
        transactionTemplate.executeWithoutResult(status -> {
            WebhookEvent event = eventRepository.findByStripeEventId(eventId).orElseGet(() -> {
                WebhookEvent created = new WebhookEvent();
                created.setStripeEventId(eventId);
                created.setEventType(eventType);
                created.setPayload(payload);
                created.setCreatedAt(LocalDateTime.now());
                return created;
            });
            event.setProcessed(false);
            event.setProcessingError(error.getClass().getSimpleName() + ": " + safeMessage(error));
            eventRepository.save(event);
        });
    }

    private void processEvent(String type, JsonNode object) {
        switch (type) {
            case "checkout.session.completed" -> checkoutCompleted(object);
            case "invoice.paid" -> invoicePaid(object);
            case "invoice.payment_failed" -> invoicePaymentFailed(object);
            case "customer.subscription.updated" -> subscriptionUpdated(object);
            case "customer.subscription.deleted" -> subscriptionDeleted(object);
            default -> log.info("stripe_webhook_ignored eventType={}", type);
        }
    }

    private void checkoutCompleted(JsonNode session) {
        String email = text(session, "client_reference_id");
        String subscriptionId = text(session, "subscription");
        String customerId = text(session, "customer");
        if (email == null || subscriptionId == null || customerId == null) {
            throw new ApiException("Checkout session is missing billing identifiers", HttpStatus.BAD_REQUEST);
        }
        Subscription subscription = subscriptionRepository.findForUpdate(email)
                .orElseGet(() -> {
                    subscriptionService.getSubscription(email);
                    return subscriptionRepository.findForUpdate(email).orElseThrow();
                });
        subscription.setStripeCustomerId(customerId);
        subscription.setStripeSubscriptionId(subscriptionId);
        String planSlug = text(session.path("metadata"), "target_plan");
        if (planSlug != null) {
            planRepository.findBySlug(planSlug.toUpperCase()).ifPresent(subscription::setPlanRecord);
        }
        subscriptionRepository.save(subscription);
    }

    private void invoicePaid(JsonNode invoice) {
        Subscription subscription = subscriptionForInvoice(invoice);
        subscription.applyStripeState(SubscriptionStatus.ACTIVE,
                subscription.getCurrentPeriodStart(), subscription.getCurrentPeriodEnd(), false);
        subscriptionRepository.save(subscription);
        savePayment(invoice, subscription, "PAID");
    }

    private void invoicePaymentFailed(JsonNode invoice) {
        Subscription subscription = subscriptionForInvoice(invoice);
        subscription.applyStripeState(SubscriptionStatus.PAST_DUE,
                subscription.getCurrentPeriodStart(), subscription.getCurrentPeriodEnd(),
                subscription.isCancelAtPeriodEnd());
        subscriptionRepository.save(subscription);
        savePayment(invoice, subscription, "FAILED");
    }

    private void subscriptionUpdated(JsonNode stripeSubscription) {
        String id = text(stripeSubscription, "id");
        Subscription subscription = subscriptionRepository.findForUpdateByStripeSubscriptionId(id)
                .orElseThrow(() -> new ApiException("Subscription not found: " + id, HttpStatus.NOT_FOUND));
        subscription.applyStripeState(mapStatus(text(stripeSubscription, "status")),
                epoch(text(stripeSubscription, "current_period_start")),
                epoch(text(stripeSubscription, "current_period_end")),
                stripeSubscription.path("cancel_at_period_end").asBoolean(false));
        String priceId = stripeSubscription.path("items").path("data").path(0)
                .path("price").path("id").asText(null);
        if (priceId != null) planRepository.findByStripePriceId(priceId).ifPresent(subscription::setPlanRecord);
        subscriptionRepository.save(subscription);
    }

    private void subscriptionDeleted(JsonNode stripeSubscription) {
        String id = text(stripeSubscription, "id");
        Subscription subscription = subscriptionRepository.findForUpdateByStripeSubscriptionId(id)
                .orElseThrow(() -> new ApiException("Subscription not found: " + id, HttpStatus.NOT_FOUND));
        subscription.cancelNow(epoch(text(stripeSubscription, "ended_at")));
        subscriptionRepository.save(subscription);
    }

    private Subscription subscriptionForInvoice(JsonNode invoice) {
        String subscriptionId = text(invoice, "subscription");
        if (subscriptionId != null) {
            return subscriptionRepository.findForUpdateByStripeSubscriptionId(subscriptionId)
                    .orElseThrow(() -> new ApiException("Subscription not found: " + subscriptionId, HttpStatus.NOT_FOUND));
        }
        String customerId = text(invoice, "customer");
        return subscriptionRepository.findForUpdateByStripeCustomerId(customerId)
                .orElseThrow(() -> new ApiException("Subscription not found for customer", HttpStatus.NOT_FOUND));
    }

    private void savePayment(JsonNode invoice, Subscription subscription, String status) {
        String invoiceId = text(invoice, "id");
        if (invoiceId != null && paymentRepository.findByStripeInvoiceId(invoiceId).isPresent()) return;
        Payment payment = new Payment();
        payment.setUserEmail(subscription.getUserEmail());
        payment.setSubscription(subscription);
        payment.setStripeInvoiceId(invoiceId);
        payment.setStripePaymentIntentId(text(invoice, "payment_intent"));
        payment.setAmountCents(invoice.path("amount_paid").asInt(invoice.path("amount_due").asInt(0)));
        payment.setCurrency(invoice.path("currency").asText("usd").toUpperCase());
        payment.setStatus(status);
        payment.setCreatedAt(LocalDateTime.now());
        paymentRepository.save(payment);
    }

    private SubscriptionStatus mapStatus(String status) {
        if (status == null) throw new ApiException("Stripe subscription status is missing", HttpStatus.BAD_REQUEST);
        return switch (status) {
            case "canceled" -> SubscriptionStatus.CANCELLED;
            case "past_due" -> SubscriptionStatus.PAST_DUE;
            case "incomplete" -> SubscriptionStatus.INCOMPLETE;
            case "incomplete_expired" -> SubscriptionStatus.INCOMPLETE_EXPIRED;
            case "trialing" -> SubscriptionStatus.TRIALING;
            case "unpaid" -> SubscriptionStatus.UNPAID;
            case "paused" -> SubscriptionStatus.PAUSED;
            default -> SubscriptionStatus.ACTIVE;
        };
    }

    private LocalDateTime epoch(String value) {
        if (value == null || value.isBlank() || "null".equals(value)) return null;
        return Instant.ofEpochSecond(Long.parseLong(value)).atZone(ZoneOffset.UTC).toLocalDateTime();
    }

    private String text(JsonNode node, String field) {
        return node.path(field).isMissingNode() || node.path(field).isNull()
                ? null : node.path(field).asText();
    }

    private String safeMessage(Exception error) {
        return error.getMessage() == null ? "no message" : error.getMessage();
    }
}
