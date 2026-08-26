package com.cloud.drive.model;

import jakarta.persistence.*;

@Entity
@Table(name = "file_ai_chunks")
public class FileAiChunk {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "file_id", nullable = false)
    private Long fileId;
    @Column(name = "chunk_index", nullable = false)
    private int chunkIndex;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    @Column(name = "embedding_json", columnDefinition = "TEXT")
    private String embeddingJson;
    @Column(name = "source_metadata", columnDefinition = "TEXT")
    private String sourceMetadata;

    public Long getId() { return id; }
    public Long getFileId() { return fileId; }
    public void setFileId(Long fileId) { this.fileId = fileId; }
    public int getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(int chunkIndex) { this.chunkIndex = chunkIndex; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getEmbeddingJson() { return embeddingJson; }
    public void setEmbeddingJson(String embeddingJson) { this.embeddingJson = embeddingJson; }
    public String getSourceMetadata() { return sourceMetadata; }
    public void setSourceMetadata(String sourceMetadata) { this.sourceMetadata = sourceMetadata; }
}
