package com.cloud.drive.repository;

import com.cloud.drive.model.FileAiChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FileAiChunkRepository extends JpaRepository<FileAiChunk, Long> {
    List<FileAiChunk> findByFileIdOrderByChunkIndexAsc(Long fileId);
    List<FileAiChunk> findByFileIdAndEmbeddingJsonIsNotNullOrderByChunkIndexAsc(Long fileId);
    void deleteByFileId(Long fileId);
}
