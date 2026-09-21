package com.cloud.drive.repository;

import com.cloud.drive.model.FileAiProcessing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface FileAiProcessingRepository extends JpaRepository<FileAiProcessing, Long> {
    @Query("SELECT p.status, COUNT(p) FROM FileAiProcessing p GROUP BY p.status ORDER BY p.status")
    List<Object[]> countByStatusGrouped();
}
