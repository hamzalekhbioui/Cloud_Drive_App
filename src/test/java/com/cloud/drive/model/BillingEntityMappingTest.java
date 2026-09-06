package com.cloud.drive.model;

import com.cloud.drive.repository.PlanRepository;
import com.cloud.drive.repository.SubscriptionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class BillingEntityMappingTest {
    @Autowired private PlanRepository planRepository;
    @Autowired private SubscriptionRepository subscriptionRepository;

    @Test
    @Transactional
    void persistsPlanAndSubscriptionLifecycleFields() {
        Plan plan = new Plan();
        plan.setName("Pro");
        plan.setSlug("PRO");
        plan.setStorageLimitBytes(50_000);
        plan.setMaxFileSizeBytes(1_000);
        plan.setMaxTeams(5);
        plan.setMaxTeamMembers(10);
        plan.setAiQueriesPerMonth(200);
        plan.setRateLimitPerMinute(500);
        plan.setPriceCents(999);
        plan.setCurrency("USD");
        plan.setBillingInterval("MONTH");
        plan.setStripePriceId("price_test_pro");
        plan.setCreatedAt(LocalDateTime.now());
        plan = planRepository.saveAndFlush(plan);

        Subscription subscription = new Subscription();
        subscription.setUserEmail("mapping@example.com");
        subscription.setPlanRecord(plan);
        subscription.setStatus(SubscriptionStatus.PAST_DUE);
        subscription.setBillingInterval(BillingInterval.MONTH);
        subscription.setCurrentPeriodStart(LocalDateTime.of(2026, 9, 1, 0, 0));
        subscription.setCurrentPeriodEnd(LocalDateTime.of(2026, 10, 1, 0, 0));
        subscription.setCancelAtPeriodEnd(true);
        subscription.setStripeCustomerId("cus_test");
        subscription.setStripeSubscriptionId("sub_test");
        subscription.setStartDate(LocalDateTime.now());
        subscription = subscriptionRepository.saveAndFlush(subscription);

        Subscription reloaded = subscriptionRepository.findById(subscription.getId()).orElseThrow();
        assertThat(reloaded.getPlanRecord().getStripePriceId()).isEqualTo("price_test_pro");
        assertThat(reloaded.getStatusValue()).isEqualTo(SubscriptionStatus.PAST_DUE);
        assertThat(reloaded.getBillingInterval()).isEqualTo(BillingInterval.MONTH);
        assertThat(reloaded.getCurrentPeriodEnd()).isEqualTo(LocalDateTime.of(2026, 10, 1, 0, 0));
        assertThat(reloaded.isCancelAtPeriodEnd()).isTrue();
        assertThat(reloaded.getStripeCustomerId()).isEqualTo("cus_test");
        assertThat(reloaded.getStripeSubscriptionId()).isEqualTo("sub_test");
    }
}
