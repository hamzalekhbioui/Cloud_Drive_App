package com.cloud.drive.repository;

import com.cloud.drive.model.FileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FileRepository extends JpaRepository<FileEntity, Long> {
    @Query("""
            SELECT f FROM FileEntity f
            WHERE (:owner IS NULL OR LOWER(f.userId) LIKE LOWER(CONCAT('%', :owner, '%')))
              AND (:status IS NULL OR f.status = :status)
              AND (:type IS NULL OR LOWER(f.type) LIKE LOWER(CONCAT('%', :type, '%')))
              AND (:minSize IS NULL OR f.size >= :minSize)
              AND (:maxSize IS NULL OR f.size <= :maxSize)
              AND (:fromDate IS NULL OR f.createdAt >= :fromDate)
              AND (:toDate IS NULL OR f.createdAt <= :toDate)
            """)
    Page<FileEntity> findAllForAdmin(@Param("owner") String owner,
                                     @Param("status") String status,
                                     @Param("type") String type,
                                     @Param("minSize") Long minSize,
                                     @Param("maxSize") Long maxSize,
                                     @Param("fromDate") LocalDateTime fromDate,
                                     @Param("toDate") LocalDateTime toDate,
                                     Pageable pageable);

    // ── existing queries ────────────────────────────────────────────────────
    List<FileEntity> findByUserIdAndDeletedAtIsNull(String userId);
    List<FileEntity> findByUserIdAndDeletedAtIsNotNull(String userId);
    List<FileEntity> findByDeletedAtBeforeAndStatusNot(LocalDateTime cutoff, String status);
    List<FileEntity> findByUserIdAndStarredTrueAndDeletedAtIsNull(String userId);
    List<FileEntity> findByTeamIdAndDeletedAtIsNull(Long teamId);
    
    @Query("SELECT f FROM FileEntity f WHERE f.teamId = :teamId AND f.deletedAt IS NULL AND (f.status = 'ACTIVE' OR f.status IS NULL)")
    List<FileEntity> findActiveByTeamId(@Param("teamId") Long teamId);

    /** PENDING uploads for a user (used during the two-phase direct upload handshake). */
    List<FileEntity> findByUserIdAndStatusAndDeletedAtIsNull(String userId, String status);
    List<FileEntity> findByStatusAndUploadExpiresAtBefore(String status, LocalDateTime cutoff);

    // ── analytics queries ───────────────────────────────────────────────────

    /** Total bytes used by active (non-trashed) files for a user. */
    @Query("SELECT COALESCE(SUM(f.size), 0) FROM FileEntity f WHERE f.userId = :userId AND f.deletedAt IS NULL")
    Long sumSizeByUser(@Param("userId") String userId);

    /** Total bytes used by all files (including trashed) for a user. */
    @Query("SELECT COALESCE(SUM(f.size), 0) FROM FileEntity f WHERE f.userId = :userId")
    Long sumSizeByUserIncludingTrash(@Param("userId") String userId);

    /** Count of active files for a user. */
    long countByUserIdAndDeletedAtIsNull(String userId);

    long countByDeletedAtIsNull();
    long countByDeletedAtIsNotNull();

    @Query("SELECT COALESCE(SUM(f.size), 0) FROM FileEntity f WHERE f.deletedAt IS NULL")
    Long sumSizeByActiveFiles();

    @Query("SELECT FUNCTION('DATE', f.createdAt), COUNT(f), COALESCE(SUM(f.size), 0) FROM FileEntity f WHERE f.createdAt >= :since AND f.deletedAt IS NULL GROUP BY FUNCTION('DATE', f.createdAt) ORDER BY FUNCTION('DATE', f.createdAt)")
    List<Object[]> countUploadsByDateSince(@Param("since") LocalDateTime since);

    @Query("SELECT f.type, COALESCE(SUM(f.size), 0) FROM FileEntity f WHERE f.deletedAt IS NULL GROUP BY f.type")
    List<Object[]> sumActiveSizeGroupedByType();

    @Query("SELECT s.plan, COALESCE(SUM(f.size), 0) FROM FileEntity f, Subscription s WHERE s.userEmail = f.userId AND f.deletedAt IS NULL GROUP BY s.plan ORDER BY s.plan")
    List<Object[]> sumActiveSizeGroupedByPlan();

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