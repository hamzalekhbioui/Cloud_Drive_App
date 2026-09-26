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
            WHERE f.userId = :userId
              AND f.deletedAt IS NULL
              AND f.status = 'ACTIVE'
              AND (:query = '' OR LOWER(f.originalFileName) LIKE LOWER(CONCAT('%', :query, '%')))
            """)
    Page<FileEntity> findVisibleByUser(@Param("userId") String userId,
                                       @Param("query") String query,
                                       Pageable pageable);

    @Query("""
            SELECT f FROM FileEntity f
            WHERE f.userId = :userId
              AND f.starred = true
              AND f.deletedAt IS NULL
              AND f.status = 'ACTIVE'
              AND (:query = '' OR LOWER(f.originalFileName) LIKE LOWER(CONCAT('%', :query, '%')))
            """)
    Page<FileEntity> findVisibleStarredByUser(@Param("userId") String userId,
                                              @Param("query") String query,
                                              Pageable pageable);

    @Query("""
            SELECT f FROM FileEntity f
            WHERE f.userId = :userId
              AND f.deletedAt IS NOT NULL
              AND f.status = 'ACTIVE'
              AND (:query = '' OR LOWER(f.originalFileName) LIKE LOWER(CONCAT('%', :query, '%')))
            """)
    Page<FileEntity> findTrashByUser(@Param("userId") String userId,
                                     @Param("query") String query,
                                     Pageable pageable);

    @Query("""
            SELECT f FROM FileEntity f
            WHERE f.teamId = :teamId
              AND f.deletedAt IS NULL
              AND f.status = 'ACTIVE'
              AND (:query = '' OR LOWER(f.originalFileName) LIKE LOWER(CONCAT('%', :query, '%')))
            """)
    Page<FileEntity> findVisibleByTeam(@Param("teamId") Long teamId,
                                       @Param("query") String query,
                                       Pageable pageable);

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
    List<FileEntity> findByDeletedAtBeforeAndStatusNot(LocalDateTime cutoff, String status);

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

    /** Daily activity is aggregated in SQL so high-volume users do not load every file row. */
    @Query(value = """
            SELECT CAST(created_at AS DATE) AS day,
                   COUNT(*) AS fileCount,
                   COALESCE(SUM(size), 0) AS totalSize
              FROM files
             WHERE user_id = :userId
               AND deleted_at IS NULL
               AND status = 'ACTIVE'
               AND created_at >= :since
             GROUP BY CAST(created_at AS DATE)
             ORDER BY CAST(created_at AS DATE)
            """, nativeQuery = true)
    List<DailyUploadAggregate> aggregateDailyUploads(@Param("userId") String userId,
                                                     @Param("since") LocalDateTime since);
}
