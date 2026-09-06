package com.cloud.drive.controller;

import com.cloud.drive.dto.billing.BillingSessionResponse;
import com.cloud.drive.dto.billing.CheckoutRequest;
import com.cloud.drive.dto.billing.UsageResponse;
import com.cloud.drive.dto.subscription.SubscriptionResponse;
import com.cloud.drive.service.BillingService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/billing")
@Tag(name = "Billing", description = "Authenticated Stripe billing and subscription operations")
public class BillingController {

    private final BillingService billingService;

    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    @PostMapping("/checkout")
    @Operation(summary = "Create a Stripe Checkout session",
            description = "Returns a Stripe-hosted checkout URL. Completion is confirmed asynchronously by Stripe webhooks.")
    @ApiResponse(responseCode = "200", description = "Checkout session created",
            content = @Content(schema = @Schema(implementation = BillingSessionResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid plan or request")
    @ApiResponse(responseCode = "503", description = "Stripe billing is not configured")
    public BillingSessionResponse createCheckout(
            @AuthenticationPrincipal UserDetails user,
            @Valid @RequestBody CheckoutRequest request) {
        return billingService.createCheckoutSession(user.getUsername(), request.getPlan());
    }

    @PostMapping("/cancel")
    @Operation(summary = "Cancel at the end of the current billing period",
            description = "Schedules cancellation in Stripe. The returned state is local state and final confirmation comes from webhooks.")
    public SubscriptionResponse cancel(@AuthenticationPrincipal UserDetails user) {
        billingService.cancelAtPeriodEnd(user.getUsername());
        return billingService.getSubscriptionState(user.getUsername());
    }

    @PostMapping("/reactivate")
    @Operation(summary = "Reactivate a scheduled cancellation")
    public SubscriptionResponse reactivate(@AuthenticationPrincipal UserDetails user) {
        billingService.reactivate(user.getUsername());
        return billingService.getSubscriptionState(user.getUsername());
    }

    @PostMapping("/portal")
    @Operation(summary = "Create a Stripe Billing Portal session")
    @ApiResponse(responseCode = "200", description = "Portal session created",
            content = @Content(schema = @Schema(implementation = BillingSessionResponse.class)))
    public BillingSessionResponse portal(@AuthenticationPrincipal UserDetails user) {
        return billingService.createPortalSession(user.getUsername());
    }

    @GetMapping("/usage")
    @Operation(summary = "Get current storage usage")
    @ApiResponse(responseCode = "200", description = "Current usage and quota",
            content = @Content(schema = @Schema(implementation = UsageResponse.class)))
    public UsageResponse usage(@AuthenticationPrincipal UserDetails user) {
        return billingService.getUsage(user.getUsername());
    }

    @GetMapping("/subscription")
    @Operation(summary = "Get the current subscription state",
            description = "Reads server-side subscription state. Checkout redirect parameters are not treated as payment confirmation.")
    @ApiResponse(responseCode = "200", description = "Current subscription state",
            content = @Content(schema = @Schema(implementation = SubscriptionResponse.class)))
    public SubscriptionResponse subscription(@AuthenticationPrincipal UserDetails user) {
        return billingService.getSubscriptionState(user.getUsername());
    }
}
