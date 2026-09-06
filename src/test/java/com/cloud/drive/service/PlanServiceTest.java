package com.cloud.drive.service;

import com.cloud.drive.exception.ApiException;
import com.cloud.drive.model.Plan;
import com.cloud.drive.repository.PlanRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanServiceTest {
    @Mock private PlanRepository planRepository;

    @Test
    void lookupIsCaseInsensitiveAndRejectsInactivePlans() {
        Plan plan = new Plan();
        plan.setSlug("PRO");
        plan.setActive(true);
        when(planRepository.findBySlug("PRO")).thenReturn(Optional.of(plan));
        PlanService service = new PlanService(planRepository, "", "", "");

        assertThat(service.getRequiredPlan("pro")).isSameAs(plan);
        plan.setActive(false);
        assertThatThrownBy(() -> service.getRequiredPlan("PRO")).isInstanceOf(ApiException.class);
    }
}
