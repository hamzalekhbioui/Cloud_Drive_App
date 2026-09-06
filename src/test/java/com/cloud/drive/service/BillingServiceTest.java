package com.cloud.drive.service;

import com.cloud.drive.config.StripeProperties;
import com.cloud.drive.exception.ApiException;
import com.cloud.drive.model.Plan;
import com.cloud.drive.repository.SubscriptionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillingServiceTest {
    @Mock private PlanService planService;
    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private SubscriptionService subscriptionService;
    @Mock private UsageService usageService;

    @Test
    void paymentOperationFailsClearlyWhenStripeSecretIsMissing() {
        BillingService service = new BillingService(new StripeProperties(), planService,
                subscriptionRepository, subscriptionService, usageService);

        assertThatThrownBy(() -> service.createCheckoutSession("free@example.com", "PRO"))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.SERVICE_UNAVAILABLE);
        verifyNoInteractions(planService, subscriptionRepository);
    }

    @Test
    void freePlanCheckoutIsBlockedWithoutTouchingStripe() {
        StripeProperties properties = new StripeProperties();
        properties.setSecretKey("sk_test_placeholder");
        Plan free = new Plan();
        free.setSlug("FREE");
        when(planService.getRequiredPlan("FREE")).thenReturn(free);
        BillingService service = new BillingService(properties, planService,
                subscriptionRepository, subscriptionService, usageService);

        assertThatThrownBy(() -> service.createCheckoutSession("free@example.com", "FREE"))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);
        verifyNoInteractions(subscriptionRepository, subscriptionService);
    }
}
