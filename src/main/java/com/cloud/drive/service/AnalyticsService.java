package com.cloud.drive.service;

import com.cloud.drive.dto.analytics.*;
import com.cloud.drive.model.FileEntity;
import com.cloud.drive.repository.FileRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    // 1 TB in bytes
    private static final long STORAGE_LIMIT = 1024L * 1024 * 1024 * 1024;

    // Category colours that mirror the frontend TYPE_COLORS palette
    private static final Map<String, String> CATEGORY_COLORS = Map.of(
            "Images",    "#6B2BB8",
            "Videos",    "#146E6B",
            "Documents", "#2B4FCC",
            "Audio",     "#B2185A",
            "Archives",  "#5A5A5E",
            "Others",    "#8A8A8E"
    );

    private final FileRepository fileRepository;

    public AnalyticsService(FileRepository fileRepository) {
        this.fileRepository = fileRepository;
    }

    // ── Overview ──────────────────────────────────────────────────────────

    public OverviewDto getOverview(String userId) {
        long used  = fileRepository.sumSizeByUser(userId);
        long files = fileRepository.countByUserIdAndDeletedAtIsNull(userId);
        double pct = STORAGE_LIMIT > 0 ? (used * 100.0) / STORAGE_LIMIT : 0;
        return new OverviewDto(used, STORAGE_LIMIT, pct, STORAGE_LIMIT - used, files);
    }

    // ── Breakdown ─────────────────────────────────────────────────────────

    public List<BreakdownItemDto> getBreakdown(String userId) {
        List<Object[]> rows = fileRepository.sumSizeGroupedByType(userId);

        // Accumulate bytes per category
        Map<String, Long> sums = new LinkedHashMap<>();
        for (Object[] row : rows) {
            String mime = (String) row[0];
            long   size = ((Number) row[1]).longValue();
            String cat  = categorize(mime);
            sums.merge(cat, size, Long::sum);
        }

        long total = sums.values().stream().mapToLong(Long::longValue).sum();
        if (total == 0) return Collections.emptyList();

        return sums.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> new BreakdownItemDto(
                        e.getKey(),
                        e.getValue(),
                        (e.getValue() * 100.0) / total,
                        CATEGORY_COLORS.getOrDefault(e.getKey(), "#8A8A8E")
                ))
                .collect(Collectors.toList());
    }

    // ── Largest Files ─────────────────────────────────────────────────────

    public List<LargestFileDto> getLargestFiles(String userId) {
        return fileRepository
                .findTop10ByUserIdAndDeletedAtIsNullOrderBySizeDesc(userId)
                .stream()
                .map(f -> new LargestFileDto(
                        f.getId(),
                        f.getOriginalFileName(),
                        f.getSize(),
                        f.getType(),
                        f.getCreatedAt() != null ? f.getCreatedAt().toString() : null
                ))
                .collect(Collectors.toList());
    }

    // ── Activity (last 30 days) ───────────────────────────────────────────

    public List<ActivityItemDto> getActivity(String userId) {
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        List<FileEntity> files = fileRepository.findActiveByUserCreatedAtAfter(userId, since);

        // Group uploaded size + count by calendar date
        Map<LocalDate, long[]> byDate = new TreeMap<>();
        for (FileEntity f : files) {
            if (f.getCreatedAt() == null) continue;
            LocalDate day = f.getCreatedAt().toLocalDate();
            byDate.computeIfAbsent(day, k -> new long[]{0, 0});
            byDate.get(day)[0] += f.getSize();  // bytes
            byDate.get(day)[1] += 1;            // count
        }

        // Fill the full 30-day window so the chart has a contiguous x-axis
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<ActivityItemDto> result = new ArrayList<>();
        LocalDate start = LocalDate.now().minusDays(29);
        for (int i = 0; i < 30; i++) {
            LocalDate day = start.plusDays(i);
            long[] vals = byDate.getOrDefault(day, new long[]{0, 0});
            result.add(new ActivityItemDto(day.format(fmt), vals[0], (int) vals[1]));
        }
        return result;
    }

    // ── Smart Insights ────────────────────────────────────────────────────

    public List<InsightDto> getInsights(String userId) {
        List<InsightDto> insights = new ArrayList<>();

        long used  = fileRepository.sumSizeByUser(userId);
        long files = fileRepository.countByUserIdAndDeletedAtIsNull(userId);

        if (used == 0) {
            insights.add(new InsightDto("info",
                    "Your vault is empty",
                    "Upload files to start seeing storage insights."));
            return insights;
        }

        // 1. Storage health
        double pct = (used * 100.0) / STORAGE_LIMIT;
        if (pct > 80) {
            insights.add(new InsightDto("warning",
                    "Storage running low",
                    String.format("You've used %.1f%% of your 1 TB — consider removing unused files.", pct)));
        } else {
            insights.add(new InsightDto("success",
                    "Storage looks healthy",
                    String.format("You've used %.1f%% of your 1 TB limit.", pct)));
        }

        // 2. Dominant category
        List<BreakdownItemDto> breakdown = getBreakdown(userId);
        if (!breakdown.isEmpty()) {
            BreakdownItemDto top = breakdown.get(0);
            insights.add(new InsightDto("info",
                    top.getCategory() + " dominate your storage",
                    String.format("%s take up %.1f%% of your used space.", top.getCategory(), top.getPercentage())));
        }

        // 3. Savings from top-3 largest files
        List<FileEntity> largest = fileRepository.findTop10ByUserIdAndDeletedAtIsNullOrderBySizeDesc(userId);
        if (largest.size() >= 3) {
            long top3 = largest.stream().limit(3).mapToLong(FileEntity::getSize).sum();
            insights.add(new InsightDto("tip",
                    "Free up " + humanReadable(top3),
                    "Deleting your 3 largest files would reclaim " + humanReadable(top3) + " of space."));
        }

        // 4. Average file size
        if (files > 0) {
            long avg = used / files;
            insights.add(new InsightDto("info",
                    "Average file size: " + humanReadable(avg),
                    String.format("You have %d active file%s totalling %s.", files, files == 1 ? "" : "s", humanReadable(used))));
        }

        return insights;
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    /** Maps a MIME type string to a human-readable category label. */
    private String categorize(String mime) {
        if (mime == null || mime.isBlank()) return "Others";
        if (mime.startsWith("image/"))                                          return "Images";
        if (mime.startsWith("video/"))                                          return "Videos";
        if (mime.startsWith("audio/"))                                          return "Audio";
        if (mime.contains("zip") || mime.contains("tar") || mime.contains("rar")
                || mime.contains("compressed") || mime.contains("archive"))     return "Archives";
        if (mime.contains("pdf")  || mime.contains("word")  || mime.contains("document")
                || mime.startsWith("text/") || mime.contains("sheet")
                || mime.contains("excel")   || mime.contains("presentation")
                || mime.contains("powerpoint"))                                  return "Documents";
        return "Others";
    }

    /** Converts bytes to a human-readable string (KB / MB / GB). */
    private String humanReadable(long bytes) {
        if (bytes < 1024)              return bytes + " B";
        if (bytes < 1024 * 1024)       return String.format("%.0f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}