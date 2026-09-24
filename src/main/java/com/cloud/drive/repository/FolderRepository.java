package com.cloud.drive.repository;

import com.cloud.drive.model.Folder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FolderRepository extends JpaRepository<Folder, Long> {
    List<Folder> findByUserIdAndTeamIdIsNullOrderByNameAsc(String userId);
    List<Folder> findByTeamIdOrderByNameAsc(Long teamId);
    boolean existsByUserIdAndTeamIdAndParentIdAndNameIgnoreCase(
            String userId, Long teamId, Long parentId, String name);
}
