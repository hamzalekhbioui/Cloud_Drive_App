package com.cloud.drive.service;

import com.cloud.drive.dto.subscription.ChangePlanRequest;
import com.cloud.drive.exception.ApiException;
import com.cloud.drive.model.Subscription;
import com.cloud.drive.repository.FileRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock private SubscriptionRepository subRepo;
    @Mock private FileRepository fileRepo;

    @InjectMocks private SubscriptionService subscriptionService;

    private static final String EMAIL = "alice@example.com";

    private Subscription subscription(String plan) {
        Subscription sub = new Subscription();
        sub.setUserEmail(EMAIL);
        sub.setPlan(plan);
        sub.setStatus("ACTIVE");
        return sub;
    }

    @Test
    void enforceStorageQuota_throwsPaymentRequired_whenUploadExceedsPlanLimit() {
        Subscription sub = subscription("FREE");
        sub.setUsedBytes(Subscription.FREE_BYTES - 1);
        when(subRepo.findForUpdate(EMAIL)).thenReturn(Optional.of(sub));

    @BeforeEach
    void setUp() {
        when(subRepo.findByUserEmail(EMAIL)).thenReturn(Optional.of(subscription("FREE")));
    }

    @Test
    void enforceStorageQuota_throwsPaymentRequired_whenUploadExceedsPlanLimit() {
        when(fileRepo.sumSizeByUser(EMAIL)).thenReturn(Subscription.FREE_BYTES - 1);

        assertThatThrownBy(() -> subscriptionService.enforceStorageQuota(EMAIL, 2))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.PAYMENT_REQUIRED);
    }

    @Test
    void changePlan_throwsConflict_whenDowngradeWouldExceedNewLimit() {
        Subscription current = subscription("BUSINESS");
        current.setUsedBytes(Subscription.FREE_BYTES + 1);
        when(subRepo.findByUserEmail(EMAIL)).thenReturn(Optional.of(current));
        when(subRepo.findByUserEmail(EMAIL)).thenReturn(Optional.of(current));
        when(fileRepo.sumSizeByUser(EMAIL)).thenReturn(Subscription.FREE_BYTES + 1);

        ChangePlanRequest req = new ChangePlanRequest();
        req.setPlan("FREE");

        assertThatThrownBy(() -> subscriptionService.changePlan(EMAIL, req))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT);

        verify(subRepo, never()).save(any());
    }
}
