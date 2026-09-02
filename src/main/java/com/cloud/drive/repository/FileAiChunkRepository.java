package com.cloud.drive.repository;

import com.cloud.drive.model.FileAiChunk;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import java.util.List;

public interface FileAiChunkRepository extends JpaRepository<FileAiChunk, Long> {
    List<FileAiChunk> findByFileIdOrderByChunkIndexAsc(Long fileId);
    List<FileAiChunk> findByFileIdAndEmbeddingJsonIsNotNullOrderByChunkIndexAsc(Long fileId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    void deleteByFileId(Long fileId);
}
