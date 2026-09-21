package com.cloud.drive.service.admin;

import com.cloud.drive.dto.admin.overview.AdminOverviewDto;
import com.cloud.drive.dto.admin.overview.GrowthPointDto;
import com.cloud.drive.dto.admin.overview.StorageOverviewDto;
import com.cloud.drive.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminOverviewServiceTest {
    @Mock private UserRepository userRepository;
    @Mock private FileRepository fileRepository;
    @Mock private FileShareRepository fileShareRepository;
    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private FileAiProcessingRepository aiProcessingRepository;
    @Mock private TeamRepository teamRepository;

    @InjectMocks private AdminOverviewService service;

    @Test
    void getOverview_aggregatesPlatformMetrics() {
        when(userRepository.count()).thenReturn(12L);
        when(userRepository.countByStatus("ACTIVE")).thenReturn(10L);
        when(userRepository.countByStatus("DISABLED")).thenReturn(2L);
        when(fileRepository.countByDeletedAtIsNull()).thenReturn(20L);
        when(fileRepository.countByDeletedAtIsNotNull()).thenReturn(3L);
        when(fileRepository.sumSizeByActiveFiles()).thenReturn(4096L);
        when(fileShareRepository.countByRevokedAtIsNull()).thenReturn(5L);
        when(fileShareRepository.countBySharedWithEmailIsNullAndRevokedAtIsNull()).thenReturn(2L);
        when(fileShareRepository.countBySharedWithEmailIsNotNullAndRevokedAtIsNull()).thenReturn(3L);
        when(teamRepository.count()).thenReturn(4L);
        when(subscriptionRepository.countActiveByPlan()).thenReturn(List.of(
                new Object[]{"PRO", 7L}, new Object[]{"FREE", 3L}));
        when(paymentRepository.sumSuccessfulAmountCents()).thenReturn(1599L);
        when(aiProcessingRepository.countByStatusGrouped()).thenReturn(List.of(
                new Object[]{"COMPLETED", 8L}, new Object[]{"PENDING", 1L}));

        AdminOverviewDto overview = service.getOverview();

        assertThat(overview.getTotalUsers()).isEqualTo(12);
        assertThat(overview.getActiveUsers()).isEqualTo(10);
        assertThat(overview.getTrashedFiles()).isEqualTo(3);
        assertThat(overview.getBytesStored()).isEqualTo(4096);
        assertThat(overview.getActiveSubscriptions()).extracting("plan").containsExactly("PRO", "FREE");
        assertThat(overview.getMrrCents()).isEqualTo(1599);
        assertThat(overview.getAiProcessing()).extracting("status").containsExactly("COMPLETED", "PENDING");
    }

    @Test
    void emptyDatabase_returnsZeroMetricsAndEmptyCollections() {
        when(subscriptionRepository.countActiveByPlan()).thenReturn(List.of());
        when(aiProcessingRepository.countByStatusGrouped()).thenReturn(List.of());
        when(paymentRepository.sumSuccessfulAmountCents()).thenReturn(0L);
        when(fileRepository.sumSizeByActiveFiles()).thenReturn(0L);

        AdminOverviewDto overview = service.getOverview();
        List<GrowthPointDto> growth = service.getGrowth();
        StorageOverviewDto storage = service.getStorage();

        assertThat(overview.getTotalUsers()).isZero();
        assertThat(overview.getBytesStored()).isZero();
        assertThat(overview.getActiveSubscriptions()).isEmpty();
        assertThat(overview.getAiProcessing()).isEmpty();
        assertThat(growth).hasSize(30).allSatisfy(point -> {
            assertThat(point.getSignups()).isZero();
            assertThat(point.getUploads()).isZero();
        });
        assertThat(storage.getByPlan()).isEmpty();
        assertThat(storage.getByFileType()).isEmpty();
    }
}
