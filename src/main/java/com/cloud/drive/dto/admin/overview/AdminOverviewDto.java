package com.cloud.drive.dto.admin.overview;

import java.util.List;

public class AdminOverviewDto {
    private final long totalUsers;
    private final long activeUsers;
    private final long disabledUsers;
    private final long totalFiles;
    private final long trashedFiles;
    private final long bytesStored;
    private final long activeShares;
    private final long publicShares;
    private final long privateShares;
    private final long teams;
    private final List<PlanCountDto> activeSubscriptions;
    private final long mrrCents;
    private final List<StatusCountDto> aiProcessing;

    public AdminOverviewDto(long totalUsers, long activeUsers, long disabledUsers,
                            long totalFiles, long trashedFiles, long bytesStored,
                            long activeShares, long publicShares, long privateShares,
                            long teams, List<PlanCountDto> activeSubscriptions,
                            long mrrCents, List<StatusCountDto> aiProcessing) {
        this.totalUsers = totalUsers;
        this.activeUsers = activeUsers;
        this.disabledUsers = disabledUsers;
        this.totalFiles = totalFiles;
        this.trashedFiles = trashedFiles;
        this.bytesStored = bytesStored;
        this.activeShares = activeShares;
        this.publicShares = publicShares;
        this.privateShares = privateShares;
        this.teams = teams;
        this.activeSubscriptions = activeSubscriptions;
        this.mrrCents = mrrCents;
        this.aiProcessing = aiProcessing;
    }

    public long getTotalUsers() { return totalUsers; }
    public long getActiveUsers() { return activeUsers; }
    public long getDisabledUsers() { return disabledUsers; }
    public long getTotalFiles() { return totalFiles; }
    public long getTrashedFiles() { return trashedFiles; }
    public long getBytesStored() { return bytesStored; }
    public long getActiveShares() { return activeShares; }
    public long getPublicShares() { return publicShares; }
    public long getPrivateShares() { return privateShares; }
    public long getTeams() { return teams; }
    public List<PlanCountDto> getActiveSubscriptions() { return activeSubscriptions; }
    public long getMrrCents() { return mrrCents; }
    public List<StatusCountDto> getAiProcessing() { return aiProcessing; }
}
