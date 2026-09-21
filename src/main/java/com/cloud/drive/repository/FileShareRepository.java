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
    Optional<FileShare> findByToken(String token);
    List<FileShare> findByFileId(Long fileId);
    List<FileShare> findBySharedWithEmail(String email);
    boolean existsByFileIdAndSharedWithEmail(Long fileId, String email);
    void deleteByFileIdAndOwnerEmail(Long fileId, String ownerEmail);
    List<FileShare> findByFileIdOrderByCreatedAtDesc(Long fileId);
    void deleteByFileId(Long fileId);
    long countByRevokedAtIsNull();
    long countBySharedWithEmailIsNullAndRevokedAtIsNull();
    long countBySharedWithEmailIsNotNullAndRevokedAtIsNull();
}