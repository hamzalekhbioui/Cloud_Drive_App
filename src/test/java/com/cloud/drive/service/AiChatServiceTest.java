package com.cloud.drive.service;

import com.cloud.drive.ai.ChatProvider;
import com.cloud.drive.ai.EmbeddingProvider;
import com.cloud.drive.dto.ChatResponse;
import com.cloud.drive.model.FileAiChunk;
import com.cloud.drive.model.FileAiProcessing;
import com.cloud.drive.model.FileEntity;
import com.cloud.drive.repository.FileAiChatMessageRepository;
import com.cloud.drive.repository.FileAiChunkRepository;
import com.cloud.drive.repository.FileAiProcessingRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiChatServiceTest {

    @Mock private FileService fileService;
    @Mock private FileAiProcessingRepository processingRepository;
    @Mock private FileAiChunkRepository chunkRepository;
    @Mock private FileAiChatMessageRepository messageRepository;
    @Mock private EmbeddingProvider embeddingProvider;
    @Mock private ChatProvider chatProvider;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void chat_usesJsonEmbeddingsToPickTheBestContext() throws Exception {
        AiChatService service = new AiChatService(
                fileService,
                processingRepository,
                chunkRepository,
                messageRepository,
                embeddingProvider,
                chatProvider,
                objectMapper
        );

        FileEntity file = file();
        FileAiProcessing processing = new FileAiProcessing(7L);
        processing.setStatus(AiProcessingService.COMPLETED);

        FileAiChunk strongMatch = chunk(7L, 0, "quarterly revenue report", "source:0", new float[]{1.0f, 0.0f});
        FileAiChunk weakMatch = chunk(7L, 1, "holiday calendar", "source:1", new float[]{0.0f, 1.0f});

        when(fileService.findOwnedForAi(7L, "alice@example.com")).thenReturn(file);
        when(processingRepository.findById(7L)).thenReturn(Optional.of(processing));
        when(embeddingProvider.isConfigured()).thenReturn(true);
        when(chatProvider.isConfigured()).thenReturn(true);
        when(embeddingProvider.embed("What is this file about?")).thenReturn(new float[]{1.0f, 0.0f});
        when(chunkRepository.findByFileIdAndEmbeddingJsonIsNotNullOrderByChunkIndexAsc(7L))
                .thenReturn(List.of(strongMatch, weakMatch));
        when(chatProvider.complete(anyList())).thenReturn("It is about revenue.");

        ChatResponse response = service.chat(7L, "alice@example.com", "What is this file about?");

        assertThat(response.getAnswer()).isEqualTo("It is about revenue.");
        assertThat(response.getCitations()).hasSize(1);
        assertThat(response.getCitations().get(0).getChunkIndex()).isZero();
        assertThat(response.getCitations().get(0).getScore()).isCloseTo(1.0, org.assertj.core.data.Offset.offset(1e-6));
    }

    @Test
    void chat_rejectsFilesWithoutSearchableChunks() {
        AiChatService service = new AiChatService(
                fileService,
                processingRepository,
                chunkRepository,
                messageRepository,
                embeddingProvider,
                chatProvider,
                objectMapper
        );

        FileEntity file = file();
        FileAiProcessing processing = new FileAiProcessing(7L);
        processing.setStatus(AiProcessingService.COMPLETED);

        when(fileService.findOwnedForAi(7L, "alice@example.com")).thenReturn(file);
        when(processingRepository.findById(7L)).thenReturn(Optional.of(processing));
        when(embeddingProvider.isConfigured()).thenReturn(true);
        when(chatProvider.isConfigured()).thenReturn(true);
        when(embeddingProvider.embed("What is this file about?")).thenReturn(new float[]{1.0f, 0.0f});
        when(chunkRepository.findByFileIdAndEmbeddingJsonIsNotNullOrderByChunkIndexAsc(7L))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.chat(7L, "alice@example.com", "What is this file about?"))
                .isInstanceOf(com.cloud.drive.exception.ApiException.class)
                .hasMessageContaining("No searchable content");
    }

    private FileEntity file() {
        FileEntity file = new FileEntity();
        file.setId(7L);
        file.setOriginalFileName("report.pdf");
        file.setType("application/pdf");
        return file;
    }

    private FileAiChunk chunk(Long fileId, int index, String content, String sourceMetadata, float[] embedding) throws Exception {
        FileAiChunk chunk = new FileAiChunk();
        chunk.setFileId(fileId);
        chunk.setChunkIndex(index);
        chunk.setContent(content);
        chunk.setSourceMetadata(sourceMetadata);
        chunk.setEmbeddingJson(objectMapper.writeValueAsString(embedding));
        return chunk;
    }
}
