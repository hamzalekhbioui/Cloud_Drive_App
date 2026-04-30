package com.cloud.drive.controller;

import com.cloud.drive.dto.subscription.ChangePlanRequest;
import com.cloud.drive.dto.subscription.SubscriptionResponse;
import com.cloud.drive.service.SubscriptionService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @GetMapping
    public SubscriptionResponse getSubscription(@AuthenticationPrincipal UserDetails ud) {
        return subscriptionService.getSubscription(ud.getUsername());
    }

    @PutMapping("/plan")
    public SubscriptionResponse changePlan(
            @Valid @RequestBody ChangePlanRequest req,
            @AuthenticationPrincipal UserDetails ud) {
        return subscriptionService.changePlan(ud.getUsername(), req);
    }
}