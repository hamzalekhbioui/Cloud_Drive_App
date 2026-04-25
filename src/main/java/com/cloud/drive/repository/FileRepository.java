package com.cloud.drive.repository;

import com.cloud.drive.model.FileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface FileRepository extends JpaRepository<FileEntity, Long> {

    // ── existing queries ────────────────────────────────────────────────────
    List<FileEntity> findByUserIdAndDeletedAtIsNull(String userId);
    List<FileEntity> findByUserIdAndDeletedAtIsNotNull(String userId);
    List<FileEntity> findByUserIdAndStarredTrueAndDeletedAtIsNull(String userId);

    // ── analytics queries ───────────────────────────────────────────────────

    /** Total bytes used by active (non-trashed) files for a user. */
    @Query("SELECT COALESCE(SUM(f.size), 0) FROM FileEntity f WHERE f.userId = :userId AND f.deletedAt IS NULL")
    Long sumSizeByUser(@Param("userId") String userId);

    /** Count of active files for a user. */
    long countByUserIdAndDeletedAtIsNull(String userId);

    /**
     * Returns [mimeType, sumOfBytes] pairs grouped by MIME type so the
     * service layer can categorise them without loading full entities.
     */
    @Query("SELECT f.type, SUM(f.size) FROM FileEntity f WHERE f.userId = :userId AND f.deletedAt IS NULL GROUP BY f.type")
    List<Object[]> sumSizeGroupedByType(@Param("userId") String userId);

    /** Top 10 largest active files for a user (Spring Data "Top" keyword). */
    List<FileEntity> findTop10ByUserIdAndDeletedAtIsNullOrderBySizeDesc(String userId);

    /** Active files uploaded on or after the given date (for activity chart). */
    @Query("SELECT f FROM FileEntity f WHERE f.userId = :userId AND f.deletedAt IS NULL AND f.createdAt >= :since ORDER BY f.createdAt ASC")
    List<FileEntity> findActiveByUserCreatedAtAfter(@Param("userId") String userId, @Param("since") LocalDateTime since);
}