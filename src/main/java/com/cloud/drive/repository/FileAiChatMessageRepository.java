package com.cloud.drive.repository;

import com.cloud.drive.model.FileAiChatMessage;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import java.util.List;

public interface FileAiChatMessageRepository extends JpaRepository<FileAiChatMessage, Long> {
    List<FileAiChatMessage> findTop10ByFileIdAndUserIdOrderByCreatedAtDesc(Long fileId, String userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    void deleteByFileId(Long fileId);
}
