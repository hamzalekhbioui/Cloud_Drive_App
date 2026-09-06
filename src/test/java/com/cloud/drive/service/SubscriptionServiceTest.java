package com.cloud.drive.service;

import com.cloud.drive.exception.ApiException;
import com.cloud.drive.model.Plan;
import com.cloud.drive.model.Subscription;
import com.cloud.drive.repository.FileRepository;
import com.cloud.drive.repository.PlanRepository;
import com.cloud.drive.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock private SubscriptionRepository subRepo;
    @Mock private FileRepository fileRepo;
    @Mock private PlanRepository planRepo;

    @InjectMocks private SubscriptionService subscriptionService;

    private static final String EMAIL = "alice@example.com";

    private Subscription subscription(String plan) {
        Subscription sub = new Subscription();
        sub.setUserEmail(EMAIL);
        sub.setPlan(plan);
        sub.setStatus("ACTIVE");
        return sub;
    }

    private Plan plan(String slug, long storageLimitBytes) {
        Plan plan = new Plan();
        plan.setSlug(slug);
        plan.setStorageLimitBytes(storageLimitBytes);
        return plan;
    }

    @Test
    void enforceStorageQuota_throwsPaymentRequired_whenUploadExceedsPlanLimit() {
        Subscription sub = subscription("FREE");
        sub.setUsedBytes(5L * 1024 * 1024 * 1024 - 1);
        when(planRepo.findBySlug("FREE")).thenReturn(Optional.of(plan("FREE", 5L * 1024 * 1024 * 1024)));
        when(subRepo.findForUpdate(EMAIL)).thenReturn(Optional.of(sub));

        assertThatThrownBy(() -> subscriptionService.enforceStorageQuota(EMAIL, 2))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.PAYMENT_REQUIRED);
    }

    @Test
    void validateStorageFitsPlan_throwsConflict_whenDowngradeWouldExceedNewLimit() {
        Subscription current = subscription("BUSINESS");
        current.setUsedBytes(5L * 1024 * 1024 * 1024 + 1);
        when(planRepo.findBySlug("FREE")).thenReturn(Optional.of(plan("FREE", 5L * 1024 * 1024 * 1024)));

        assertThatThrownBy(() -> subscriptionService.validateStorageFitsPlan(current, "FREE"))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT);

        verify(subRepo, never()).save(any());
    }

    @Test
    void getSubscription_autoProvisionsFreePlanWithoutStripeConfiguration() {
        Plan free = plan("FREE", 5L * 1024 * 1024 * 1024);
        when(subRepo.findByUserEmail(EMAIL)).thenReturn(java.util.Optional.empty());
        when(planRepo.findBySlug("FREE")).thenReturn(java.util.Optional.of(free));
        when(subRepo.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = subscriptionService.getSubscription(EMAIL);

        assertThat(response.getPlan()).isEqualTo("FREE");
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
        verify(subRepo).save(any(Subscription.class));
    }
}
