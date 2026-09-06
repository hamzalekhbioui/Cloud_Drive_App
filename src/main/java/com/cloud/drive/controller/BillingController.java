package com.cloud.drive.controller;

import com.cloud.drive.dto.billing.BillingSessionResponse;
import com.cloud.drive.dto.billing.CheckoutRequest;
import com.cloud.drive.service.BillingService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/billing")
public class BillingController {

    private final BillingService billingService;

    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    @PostMapping("/checkout")
    public BillingSessionResponse createCheckout(
            @AuthenticationPrincipal UserDetails user,
            @Valid @RequestBody CheckoutRequest request) {
        return billingService.createCheckoutSession(user.getUsername(), request.getPlan());
    }

    @PostMapping("/cancel")
    public void cancel(@AuthenticationPrincipal UserDetails user) {
        billingService.cancelAtPeriodEnd(user.getUsername());
    }

    @PostMapping("/reactivate")
    public void reactivate(@AuthenticationPrincipal UserDetails user) {
        billingService.reactivate(user.getUsername());
    }

    @PostMapping("/portal")
    public BillingSessionResponse portal(@AuthenticationPrincipal UserDetails user) {
        return billingService.createPortalSession(user.getUsername());
    }
}
