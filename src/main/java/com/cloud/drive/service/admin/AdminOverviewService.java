package com.cloud.drive.service.admin;

import com.cloud.drive.dto.admin.overview.*;
import com.cloud.drive.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class AdminOverviewService {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private final UserRepository userRepository;
    private final FileRepository fileRepository;
    private final FileShareRepository fileShareRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;
    private final FileAiProcessingRepository aiProcessingRepository;
    private final TeamRepository teamRepository;

    public AdminOverviewService(UserRepository userRepository,
                                FileRepository fileRepository,
                                FileShareRepository fileShareRepository,
                                SubscriptionRepository subscriptionRepository,
                                PaymentRepository paymentRepository,
                                FileAiProcessingRepository aiProcessingRepository,
                                TeamRepository teamRepository) {
        this.userRepository = userRepository;
        this.fileRepository = fileRepository;
        this.fileShareRepository = fileShareRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.paymentRepository = paymentRepository;
        this.aiProcessingRepository = aiProcessingRepository;
        this.teamRepository = teamRepository;
    }

    @Transactional(readOnly = true)
    public AdminOverviewDto getOverview() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByStatus("ACTIVE");
        long disabledUsers = userRepository.countByStatus("DISABLED");
        long totalFiles = fileRepository.countByDeletedAtIsNull();
        long trashedFiles = fileRepository.countByDeletedAtIsNotNull();
        long bytesStored = number(fileRepository.sumSizeByActiveFiles());
        long activeShares = fileShareRepository.countByRevokedAtIsNull();
        long publicShares = fileShareRepository.countBySharedWithEmailIsNullAndRevokedAtIsNull();
        long privateShares = fileShareRepository.countBySharedWithEmailIsNotNullAndRevokedAtIsNull();

        List<PlanCountDto> plans = subscriptionRepository.countActiveByPlan().stream()
                .map(row -> new PlanCountDto(String.valueOf(row[0]), number(row[1])))
                .toList();
        List<StatusCountDto> ai = aiProcessingRepository.countByStatusGrouped().stream()
                .map(row -> new StatusCountDto(String.valueOf(row[0]), number(row[1])))
                .toList();

        return new AdminOverviewDto(
                totalUsers, activeUsers, disabledUsers, totalFiles, trashedFiles,
                bytesStored, activeShares, publicShares, privateShares, teamRepository.count(),
                plans, number(paymentRepository.sumSuccessfulAmountCents()), ai);
    }

    @Transactional(readOnly = true)
    public List<GrowthPointDto> getGrowth() {
        LocalDate start = LocalDate.now().minusDays(29);
        Map<LocalDate, long[]> points = new TreeMap<>();
        for (int i = 0; i < 30; i++) points.put(start.plusDays(i), new long[3]);

        for (Object[] row : userRepository.countSignupsByDateSince(start.atStartOfDay())) {
            long[] point = points.get(date(row[0]));
            if (point != null) point[0] = number(row[1]);
        }
        for (Object[] row : fileRepository.countUploadsByDateSince(start.atStartOfDay())) {
            long[] point = points.get(date(row[0]));
            if (point != null) {
                point[1] = number(row[1]);
                point[2] = number(row[2]);
            }
        }

        return points.entrySet().stream()
                .map(entry -> new GrowthPointDto(entry.getKey().format(DATE_FORMAT),
                        entry.getValue()[0], entry.getValue()[1], entry.getValue()[2]))
                .toList();
    }

    @Transactional(readOnly = true)
    public StorageOverviewDto getStorage() {
        List<StoragePointDto> byPlan = fileRepository.sumActiveSizeGroupedByPlan().stream()
                .map(row -> new StoragePointDto(String.valueOf(row[0]), number(row[1])))
                .toList();

        Map<String, Long> categories = new HashMap<>();
        for (Object[] row : fileRepository.sumActiveSizeGroupedByType()) {
            categories.merge(categorize(row[0] == null ? null : String.valueOf(row[0])), number(row[1]), Long::sum);
        }
        List<StoragePointDto> byFileType = categories.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(entry -> new StoragePointDto(entry.getKey(), entry.getValue()))
                .toList();
        return new StorageOverviewDto(byPlan, byFileType);
    }

    private static long number(Object value) {
        return value == null ? 0 : ((Number) value).longValue();
    }

    private static LocalDate date(Object value) {
        if (value instanceof LocalDate localDate) return localDate;
        if (value instanceof java.sql.Date sqlDate) return sqlDate.toLocalDate();
        return LocalDate.parse(String.valueOf(value));
    }

    private static String categorize(String mime) {
        if (mime == null || mime.isBlank()) return "Others";
        if (mime.startsWith("image/")) return "Images";
        if (mime.startsWith("video/")) return "Videos";
        if (mime.startsWith("audio/")) return "Audio";
        if (mime.contains("zip") || mime.contains("tar") || mime.contains("rar")
                || mime.contains("compressed") || mime.contains("archive")) return "Archives";
        if (mime.contains("pdf") || mime.contains("word") || mime.contains("document")
                || mime.startsWith("text/") || mime.contains("sheet")
                || mime.contains("excel") || mime.contains("presentation")
                || mime.contains("powerpoint")) return "Documents";
        return "Others";
    }
}
