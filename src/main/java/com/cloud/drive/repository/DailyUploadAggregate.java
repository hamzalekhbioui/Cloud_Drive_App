package com.cloud.drive.repository;

import java.time.LocalDate;

/** Database-side daily upload aggregate used by the activity chart. */
public interface DailyUploadAggregate {
    LocalDate getDay();
    long getFileCount();
    long getTotalSize();
}
