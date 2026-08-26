package com.cloud.drive.repository;

import com.cloud.drive.model.FileAiChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FileAiChatMessageRepository extends JpaRepository<FileAiChatMessage, Long> {
    List<FileAiChatMessage> findTop10ByFileIdAndUserIdOrderByCreatedAtDesc(Long fileId, String userId);
    void deleteByFileId(Long fileId);
}
