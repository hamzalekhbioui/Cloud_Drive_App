package com.cloud.drive.repository;

import com.cloud.drive.model.FileShare;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FileShareRepository extends JpaRepository<FileShare, Long> {
    Optional<FileShare> findByToken(String token);
    List<FileShare> findByFileId(Long fileId);
    List<FileShare> findBySharedWithEmail(String email);
    boolean existsByFileIdAndSharedWithEmail(Long fileId, String email);
    void deleteByFileIdAndOwnerEmail(Long fileId, String ownerEmail);
}