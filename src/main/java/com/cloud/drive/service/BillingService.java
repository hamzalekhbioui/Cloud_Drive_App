package com.cloud.drive.service;

import com.cloud.drive.config.StripeProperties;
import com.cloud.drive.exception.ApiException;
import com.cloud.drive.model.Plan;
import com.cloud.drive.model.Subscription;
import com.cloud.drive.repository.SubscriptionRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.billingportal.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.SubscriptionUpdateParams;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.nio.charset.StandardCharsets;

@Service
public class BillingService {

    private final StripeProperties stripeProperties;
    private final PlanService planService;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionService subscriptionService;

    public BillingService(StripeProperties stripeProperties,
                          PlanService planService,
                          SubscriptionRepository subscriptionRepository,
                          SubscriptionService subscriptionService) {
        this.stripeProperties = stripeProperties;
        this.planService = planService;
        this.subscriptionRepository = subscriptionRepository;
        this.subscriptionService = subscriptionService;
    }

    @Transactional
    public com.cloud.drive.dto.billing.BillingSessionResponse createCheckoutSession(
            String userEmail, String targetPlan) {
        requireStripeConfigured();
        Plan plan = planService.getRequiredPlan(targetPlan);
        if ("FREE".equals(plan.getSlug())) {
            throw new ApiException("FREE does not require checkout", HttpStatus.BAD_REQUEST);
        }
        if (plan.getStripePriceId() == null || plan.getStripePriceId().isBlank()) {
            throw new ApiException("Stripe price is not configured for " + plan.getSlug(),
                    HttpStatus.SERVICE_UNAVAILABLE);
        }

        Subscription subscription = getOrCreateSubscription(userEmail);
        String customerId = ensureCustomer(subscription, userEmail);
        RequestOptions options = idempotent("checkout:" + userEmail + ":" + plan.getSlug());

        try {
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .setCustomer(customerId)
                    .setClientReferenceId(userEmail)
                    .putMetadata("target_plan", plan.getSlug())
                    .setSuccessUrl(stripeProperties.getFrontendSuccessUrl())
                    .setCancelUrl(stripeProperties.getFrontendCancelUrl())
                    .addLineItem(SessionCreateParams.LineItem.builder()
                            .setPrice(plan.getStripePriceId())
                            .setQuantity(1L)
                            .build())
                    .build();
            com.stripe.model.checkout.Session session =
                    com.stripe.model.checkout.Session.create(params, options);
            return new com.cloud.drive.dto.billing.BillingSessionResponse(session.getId(), session.getUrl());
        } catch (StripeException e) {
            throw stripeFailure("Unable to create checkout session", e);
        }
    }

    @Transactional
    public void cancelAtPeriodEnd(String userEmail) {
        Subscription subscription = findBillableSubscription(userEmail);
        requireStripeConfigured();
        if (subscription.getStripeSubscriptionId() == null) {
            throw new ApiException("No Stripe subscription is active", HttpStatus.CONFLICT);
        }
        try {
            com.stripe.model.Subscription stripeSubscription =
                    com.stripe.model.Subscription.retrieve(
                            subscription.getStripeSubscriptionId(), apiOptions());
            stripeSubscription.update(
                    SubscriptionUpdateParams.builder().setCancelAtPeriodEnd(true).build(),
                    idempotent("cancel:" + subscription.getStripeSubscriptionId()));
            subscriptionService.scheduleCancellation(userEmail);
        } catch (StripeException e) {
            throw stripeFailure("Unable to schedule subscription cancellation", e);
        }
    }

    @Transactional
    public void reactivate(String userEmail) {
        Subscription subscription = findBillableSubscription(userEmail);
        requireStripeConfigured();
        if (subscription.getStripeSubscriptionId() == null) {
            throw new ApiException("No Stripe subscription is active", HttpStatus.CONFLICT);
        }
        try {
            com.stripe.model.Subscription stripeSubscription =
                    com.stripe.model.Subscription.retrieve(
                            subscription.getStripeSubscriptionId(), apiOptions());
            stripeSubscription.update(
                    SubscriptionUpdateParams.builder().setCancelAtPeriodEnd(false).build(),
                    idempotent("reactivate:" + subscription.getStripeSubscriptionId()));
            subscriptionService.reactivate(userEmail);
        } catch (StripeException e) {
            throw stripeFailure("Unable to reactivate subscription", e);
        }
    }

    public com.cloud.drive.dto.billing.BillingSessionResponse createPortalSession(String userEmail) {
        Subscription subscription = findBillableSubscription(userEmail);
        requireStripeConfigured();
        if (subscription.getStripeCustomerId() == null) {
            throw new ApiException("No Stripe customer is available", HttpStatus.CONFLICT);
        }
        try {
            Session session = Session.create(
                    com.stripe.param.billingportal.SessionCreateParams.builder()
                            .setCustomer(subscription.getStripeCustomerId())
                            .setReturnUrl(stripeProperties.getFrontendSuccessUrl())
                            .build(),
                    idempotent("portal:" + userEmail));
            return new com.cloud.drive.dto.billing.BillingSessionResponse(session.getId(), session.getUrl());
        } catch (StripeException e) {
            throw stripeFailure("Unable to create billing portal session", e);
        }
    }

    private Subscription getOrCreateSubscription(String userEmail) {
        subscriptionService.getSubscription(userEmail);
        return subscriptionRepository.findForUpdate(userEmail)
                .orElseThrow(() -> new ApiException("Subscription not found", HttpStatus.INTERNAL_SERVER_ERROR));
    }

    private Subscription findBillableSubscription(String userEmail) {
        return subscriptionRepository.findForUpdate(userEmail)
                .orElseThrow(() -> new ApiException("Subscription not found", HttpStatus.NOT_FOUND));
    }

    private String ensureCustomer(Subscription subscription, String userEmail) {
        if (subscription.getStripeCustomerId() != null && !subscription.getStripeCustomerId().isBlank()) {
            return subscription.getStripeCustomerId();
        }
        try {
            Customer customer = Customer.create(
                    CustomerCreateParams.builder()
                            .setEmail(userEmail)
                            .putMetadata("app_user_email", userEmail)
                            .build(),
                    idempotent("customer:" + userEmail));
            subscription.setStripeCustomerId(customer.getId());
            subscriptionRepository.save(subscription);
            return customer.getId();
        } catch (StripeException e) {
            throw stripeFailure("Unable to create Stripe customer", e);
        }
    }

    private RequestOptions idempotent(String operation) {
        return RequestOptions.builder()
                .setApiKey(stripeProperties.getSecretKey())
                .setIdempotencyKey(operation + ":" + UUID.nameUUIDFromBytes(
                        operation.getBytes(StandardCharsets.UTF_8)))
                .build();
    }

    private RequestOptions apiOptions() {
        return RequestOptions.builder()
                .setApiKey(stripeProperties.getSecretKey())
                .build();
    }

    private void requireStripeConfigured() {
        if (stripeProperties.getSecretKey() == null || stripeProperties.getSecretKey().isBlank()) {
            throw new ApiException("Stripe billing is not configured", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private ApiException stripeFailure(String message, StripeException cause) {
        return new ApiException(message, HttpStatus.BAD_GATEWAY);
    }
}
