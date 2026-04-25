package com.cloud.drive.repository;

import com.cloud.drive.model.FileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FileRepository extends JpaRepository<FileEntity, Long> {
    List<FileEntity> findByUserIdAndDeletedAtIsNull(String userId);
    List<FileEntity> findByUserIdAndDeletedAtIsNotNull(String userId);
    List<FileEntity> findByUserIdAndStarredTrueAndDeletedAtIsNull(String userId);
}
