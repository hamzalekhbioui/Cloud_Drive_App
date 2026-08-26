package com.cloud.drive.repository;

import com.cloud.drive.model.FileAiChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface FileAiChunkRepository extends JpaRepository<FileAiChunk, Long> {
    List<FileAiChunk> findByFileIdOrderByChunkIndexAsc(Long fileId);
    void deleteByFileId(Long fileId);

    @Query(value = """
            SELECT id, file_id AS fileId, chunk_index AS chunkIndex, content,
                   source_metadata AS sourceMetadata,
                   (1 - (embedding <=> CAST(:embedding AS vector))) AS score
            FROM file_ai_chunks
            WHERE file_id = :fileId
              AND embedding IS NOT NULL
              AND (1 - (embedding <=> CAST(:embedding AS vector))) >= :threshold
            ORDER BY embedding <=> CAST(:embedding AS vector)
            LIMIT :limit
            """, nativeQuery = true)
    List<FileAiChunkSearchRow> searchSimilar(Long fileId, String embedding, double threshold, int limit);

    interface FileAiChunkSearchRow {
        Long getId();
        Long getFileId();
        Integer getChunkIndex();
        String getContent();
        String getSourceMetadata();
        Double getScore();
    }
}
