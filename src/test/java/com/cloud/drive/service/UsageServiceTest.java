package com.cloud.drive.service;

import com.cloud.drive.exception.ApiException;
import com.cloud.drive.model.Plan;
import com.cloud.drive.model.UsageTracking;
import com.cloud.drive.repository.UsageTrackingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsageServiceTest {
    @Mock private UsageTrackingRepository usageRepository;
    @Mock private SubscriptionService subscriptionService;

    private UsageService service(String now) {
        return new UsageService(usageRepository, subscriptionService,
                Clock.fixed(Instant.parse(now), ZoneOffset.UTC));
    }

    private Plan plan(int limit) {
        Plan plan = new Plan();
        plan.setAiQueriesPerMonth(limit);
        plan.setStorageLimitBytes(100);
        return plan;
    }

    @Test
    void consumeAiQuery_rollsIntoNewCalendarPeriod() {
        when(subscriptionService.getPlanForUser("alice")).thenReturn(plan(10));
        when(usageRepository.findForUpdate(eq("alice"), eq(UsageService.AI_QUERY), any()))
                .thenReturn(Optional.empty());
        when(usageRepository.save(any(UsageTracking.class))).thenAnswer(inv -> inv.getArgument(0));

        service("2026-02-01T00:00:00Z").consumeAiQuery("alice");

        ArgumentCaptor<UsageTracking> captor = ArgumentCaptor.forClass(UsageTracking.class);
        verify(usageRepository, times(2)).save(captor.capture());
        UsageTracking saved = captor.getAllValues().get(1);
        assertThat(saved.getPeriodStart().toString()).isEqualTo("2026-02-01");
        assertThat(saved.getPeriodEnd().toString()).isEqualTo("2026-02-28");
        assertThat(saved.getUsageCount()).isEqualTo(1);
    }

    @Test
    void consumeAiQuery_allowsUnlimitedBusinessPlan() {
        when(subscriptionService.getPlanForUser("alice")).thenReturn(plan(-1));
        UsageTracking usage = new UsageTracking();
        usage.setUsageCount(100_000);
        when(usageRepository.findForUpdate(eq("alice"), eq(UsageService.AI_QUERY), any()))
                .thenReturn(Optional.of(usage));

        service("2026-03-15T00:00:00Z").consumeAiQuery("alice");

        assertThat(usage.getUsageCount()).isEqualTo(100_001);
    }

    @Test
    void consumeAiQuery_rejectsLimitWithPaymentRequired() {
        when(subscriptionService.getPlanForUser("alice")).thenReturn(plan(10));
        UsageTracking usage = new UsageTracking();
        usage.setUsageCount(10);
        when(usageRepository.findForUpdate(eq("alice"), eq(UsageService.AI_QUERY), any()))
                .thenReturn(Optional.of(usage));

        assertThatThrownBy(() -> service("2026-03-15T00:00:00Z").consumeAiQuery("alice"))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.PAYMENT_REQUIRED);
        verify(usageRepository, never()).save(any());
    }

    @Test
    void consumeAiQuery_usesLockedPeriodRowForConcurrentSafeIncrement() {
        when(subscriptionService.getPlanForUser("alice")).thenReturn(plan(10));
        UsageTracking usage = new UsageTracking();
        usage.setUsageCount(1);
        when(usageRepository.findForUpdate(eq("alice"), eq(UsageService.AI_QUERY), any()))
                .thenReturn(Optional.of(usage));

        service("2026-03-15T00:00:00Z").consumeAiQuery("alice");

        verify(usageRepository).findForUpdate(eq("alice"), eq(UsageService.AI_QUERY), any());
        verify(usageRepository).save(usage);
        assertThat(usage.getUsageCount()).isEqualTo(2);
    }
}
