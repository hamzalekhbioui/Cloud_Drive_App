package com.cloud.drive.service;

import com.cloud.drive.dto.analytics.ActivityItemDto;
import com.cloud.drive.dto.analytics.BreakdownItemDto;
import com.cloud.drive.model.FileEntity;
import com.cloud.drive.repository.FileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock private FileRepository fileRepository;
    @Mock private SubscriptionService subscriptionService;

    @InjectMocks private AnalyticsService analyticsService;

    private static final String EMAIL = "alice@example.com";

    @Test
    void getBreakdown_categorizesMimeTypes() {
        when(fileRepository.sumSizeGroupedByType(EMAIL)).thenReturn(List.of(
                new Object[]{"image/png", 200L},
                new Object[]{"application/pdf", 150L},
                new Object[]{"application/zip", 100L},
                new Object[]{null, 50L}
        ));

        List<BreakdownItemDto> breakdown = analyticsService.getBreakdown(EMAIL);

        assertThat(breakdown).extracting(BreakdownItemDto::getCategory)
                .containsExactly("Images", "Documents", "Archives", "Others");
        assertThat(breakdown.get(0).getSize()).isEqualTo(200L);
        assertThat(breakdown.get(0).getPercentage()).isEqualTo(40.0);
    }

    @Test
    void getActivity_fillsFullThirtyDayWindow() {
        LocalDate today = LocalDate.now();
        FileEntity early = file(today.minusDays(29).atTime(10, 0), 10L);
        FileEntity late = file(today.atTime(12, 0), 30L);

        when(fileRepository.findActiveByUserCreatedAtAfter(anyString(), org.mockito.ArgumentMatchers.any(LocalDateTime.class)))
                .thenReturn(List.of(early, late));

        List<ActivityItemDto> activity = analyticsService.getActivity(EMAIL);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        assertThat(activity).hasSize(30);
        assertThat(activity.get(0).getDate()).isEqualTo(today.minusDays(29).format(fmt));
        assertThat(activity.get(29).getDate()).isEqualTo(today.format(fmt));
        assertThat(activity.get(0).getTotalUploadedSize()).isEqualTo(10L);
        assertThat(activity.get(0).getFileCount()).isEqualTo(1);
        assertThat(activity.get(29).getTotalUploadedSize()).isEqualTo(30L);
        assertThat(activity.get(29).getFileCount()).isEqualTo(1);
    }

    private FileEntity file(LocalDateTime createdAt, long size) {
        FileEntity file = new FileEntity();
        file.setCreatedAt(createdAt);
        file.setSize(size);
        file.setType("application/pdf");
        return file;
    }
}
