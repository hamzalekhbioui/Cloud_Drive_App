package com.cloud.drive.service;

import com.cloud.drive.dto.plan.PlanResponse;
import com.cloud.drive.exception.ApiException;
import com.cloud.drive.model.Plan;
import com.cloud.drive.repository.PlanRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PlanService {

    private final PlanRepository planRepository;
    private final String freeStripePriceId;
    private final String proStripePriceId;
    private final String businessStripePriceId;

    public PlanService(
            PlanRepository planRepository,
            @Value("${stripe.price-id.free:}") String freeStripePriceId,
            @Value("${stripe.price-id.pro:}") String proStripePriceId,
            @Value("${stripe.price-id.business:}") String businessStripePriceId) {
        this.planRepository = planRepository;
        this.freeStripePriceId = freeStripePriceId;
        this.proStripePriceId = proStripePriceId;
        this.businessStripePriceId = businessStripePriceId;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void applyConfiguredStripePriceIds() {
        updateStripePriceId("FREE", freeStripePriceId);
        updateStripePriceId("PRO", proStripePriceId);
        updateStripePriceId("BUSINESS", businessStripePriceId);
    }

    @Transactional(readOnly = true)
    public List<PlanResponse> getActivePlans() {
        return planRepository.findByActiveTrueOrderByIdAsc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Plan getRequiredPlan(String slug) {
        return planRepository.findBySlug(slug.toUpperCase())
                .filter(Plan::isActive)
                .orElseThrow(() -> new ApiException("Plan not found: " + slug, HttpStatus.INTERNAL_SERVER_ERROR));
    }

    private void updateStripePriceId(String slug, String configuredId) {
        if (configuredId == null || configuredId.isBlank()) return;
        planRepository.findBySlug(slug).ifPresent(plan -> {
            plan.setStripePriceId(configuredId);
            planRepository.save(plan);
        });
    }

    private PlanResponse toResponse(Plan plan) {
        PlanResponse response = new PlanResponse();
        response.setName(plan.getName());
        response.setSlug(plan.getSlug());
        response.setStorageLimitBytes(plan.getStorageLimitBytes());
        response.setMaxFileSizeBytes(plan.getMaxFileSizeBytes());
        response.setMaxTeams(plan.getMaxTeams());
        response.setMaxTeamMembers(plan.getMaxTeamMembers());
        response.setAiQueriesPerMonth(plan.getAiQueriesPerMonth());
        response.setRateLimitPerMinute(plan.getRateLimitPerMinute());
        response.setPriceCents(plan.getPriceCents());
        response.setCurrency(plan.getCurrency());
        response.setBillingInterval(plan.getBillingInterval());
        return response;
    }
}
