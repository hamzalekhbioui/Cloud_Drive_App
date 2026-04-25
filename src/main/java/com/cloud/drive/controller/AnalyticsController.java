package com.cloud.drive.controller;

import com.cloud.drive.dto.analytics.*;
import com.cloud.drive.service.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    /** GET /api/analytics/overview — totals, limit, % used, remaining */
    @GetMapping("/overview")
    public ResponseEntity<OverviewDto> overview(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(analyticsService.getOverview(userDetails.getUsername()));
    }

    /** GET /api/analytics/breakdown — bytes per file-type category */
    @GetMapping("/breakdown")
    public ResponseEntity<List<BreakdownItemDto>> breakdown(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(analyticsService.getBreakdown(userDetails.getUsername()));
    }

    /** GET /api/analytics/largest-files — top 10 files by size */
    @GetMapping("/largest-files")
    public ResponseEntity<List<LargestFileDto>> largestFiles(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(analyticsService.getLargestFiles(userDetails.getUsername()));
    }

    /** GET /api/analytics/activity — daily upload activity for the last 30 days */
    @GetMapping("/activity")
    public ResponseEntity<List<ActivityItemDto>> activity(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(analyticsService.getActivity(userDetails.getUsername()));
    }

    /** GET /api/analytics/insights — smart storage insights */
    @GetMapping("/insights")
    public ResponseEntity<List<InsightDto>> insights(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(analyticsService.getInsights(userDetails.getUsername()));
    }
}
