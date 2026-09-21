package com.cloud.drive.dto.admin.overview;

import java.util.List;

public class StorageOverviewDto {
    private final List<StoragePointDto> byPlan;
    private final List<StoragePointDto> byFileType;

    public StorageOverviewDto(List<StoragePointDto> byPlan, List<StoragePointDto> byFileType) {
        this.byPlan = byPlan;
        this.byFileType = byFileType;
    }

    public List<StoragePointDto> getByPlan() { return byPlan; }
    public List<StoragePointDto> getByFileType() { return byFileType; }
}
