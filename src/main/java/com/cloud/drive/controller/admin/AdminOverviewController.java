package com.cloud.drive.controller.admin;

import com.cloud.drive.dto.admin.overview.*;
import com.cloud.drive.service.admin.AdminOverviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminOverviewController {
    private final AdminOverviewService overviewService;

    public AdminOverviewController(AdminOverviewService overviewService) {
        this.overviewService = overviewService;
    }

    @GetMapping("/overview")
    public ResponseEntity<AdminOverviewDto> overview() {
        return ResponseEntity.ok(overviewService.getOverview());
    }

    @GetMapping("/overview/growth")
    public ResponseEntity<List<GrowthPointDto>> growth() {
        return ResponseEntity.ok(overviewService.getGrowth());
    }

    @GetMapping("/overview/storage")
    public ResponseEntity<StorageOverviewDto> storage() {
        return ResponseEntity.ok(overviewService.getStorage());
    }
}
