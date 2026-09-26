package com.cloud.drive.repository;

import com.cloud.drive.model.FileShare;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FileShareRepository extends JpaRepository<FileShare, Long> {
    @Query("SELECT s FROM FileShare s WHERE (:owner IS NULL OR LOWER(s.ownerEmail) LIKE LOWER(CONCAT('%', :owner, '%'))) AND (:revoked IS NULL OR (:revoked = true AND s.revokedAt IS NOT NULL) OR (:revoked = false AND s.revokedAt IS NULL))")
    Page<FileShare> findAllForAdmin(@Param("owner") String owner, @Param("revoked") Boolean revoked, Pageable pageable);
    @Query("""
            SELECT s.id AS id,
                   s.fileId AS fileId,
                   f.originalFileName AS fileName,
                   s.ownerEmail AS ownerEmail,
                   s.permission AS permission,
                   s.createdAt AS createdAt,
                   s.expiresAt AS expiresAt,
                   f.size AS size,
                   f.type AS type
              FROM FileShare s
              JOIN FileEntity f ON f.id = s.fileId
             WHERE LOWER(s.sharedWithEmail) = LOWER(:email)
               AND s.revokedAt IS NULL
               AND (s.expiresAt IS NULL OR s.expiresAt > :now)
               AND f.deletedAt IS NULL
               AND f.status = 'ACTIVE'
            """)
    Page<SharedFileView> findAvailableSharedWith(@Param("email") String email,
                                                 @Param("now") java.time.LocalDateTime now,
                                                 Pageable pageable);
    Optional<FileShare> findByToken(String token);
    List<FileShare> findTop100ByFileIdOrderByCreatedAtDesc(Long fileId);
    List<FileShare> findTop100ByFileIdAndRevokedAtIsNullOrderByCreatedAtDesc(Long fileId);
    boolean existsByFileIdAndSharedWithEmail(Long fileId, String email);
    void deleteByFileIdAndOwnerEmail(Long fileId, String ownerEmail);
    List<FileShare> findByFileIdOrderByCreatedAtDesc(Long fileId);
    List<FileShare> findByFileIdInOrderByCreatedAtDesc(List<Long> fileIds);
    void deleteByFileId(Long fileId);
    long countByRevokedAtIsNull();
    long countBySharedWithEmailIsNullAndRevokedAtIsNull();
    long countBySharedWithEmailIsNotNullAndRevokedAtIsNull();
}
