package com.cloud.drive.service;

import com.cloud.drive.ai.ChatProvider;
import com.cloud.drive.ai.EmbeddingProvider;
import com.cloud.drive.dto.AiCitationDto;
import com.cloud.drive.dto.AiStatusDto;
import com.cloud.drive.dto.ChatResponse;
import com.cloud.drive.exception.ApiException;
import com.cloud.drive.model.FileAiChunk;
import com.cloud.drive.model.FileAiChatMessage;
import com.cloud.drive.model.FileAiProcessing;
import com.cloud.drive.model.FileEntity;
import com.cloud.drive.repository.FileAiChatMessageRepository;
import com.cloud.drive.repository.FileAiChunkRepository;
import com.cloud.drive.repository.FileAiProcessingRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class AiChatService {
    private final FileService fileService;
    private final FileAiProcessingRepository processingRepository;
    private final FileAiChunkRepository chunkRepository;
    private final FileAiChatMessageRepository messageRepository;
    private final EmbeddingProvider embeddingProvider;
    private final ChatProvider chatProvider;
    private final ObjectMapper objectMapper;
    private final AiProcessingService aiProcessingService;
    private final UsageService usageService;

    public AiChatService(FileService fileService,
                         FileAiProcessingRepository processingRepository,
                         FileAiChunkRepository chunkRepository,
                         FileAiChatMessageRepository messageRepository,
                         EmbeddingProvider embeddingProvider,
                         ChatProvider chatProvider,
                         ObjectMapper objectMapper) {
        this(fileService, processingRepository, chunkRepository, messageRepository,
                embeddingProvider, chatProvider, objectMapper, null, null);
    }

    public AiChatService(FileService fileService,
                         FileAiProcessingRepository processingRepository,
                         FileAiChunkRepository chunkRepository,
                         FileAiChatMessageRepository messageRepository,
                         EmbeddingProvider embeddingProvider,
                         ChatProvider chatProvider,
                         ObjectMapper objectMapper,
                         AiProcessingService aiProcessingService) {
        this(fileService, processingRepository, chunkRepository, messageRepository,
                embeddingProvider, chatProvider, objectMapper, aiProcessingService, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public AiChatService(FileService fileService,
                         FileAiProcessingRepository processingRepository,
                         FileAiChunkRepository chunkRepository,
                         FileAiChatMessageRepository messageRepository,
                         EmbeddingProvider embeddingProvider,
                         ChatProvider chatProvider,
                         ObjectMapper objectMapper,
                         AiProcessingService aiProcessingService,
                         UsageService usageService) {
        this.fileService = fileService;
        this.processingRepository = processingRepository;
        this.chunkRepository = chunkRepository;
        this.messageRepository = messageRepository;
        this.embeddingProvider = embeddingProvider;
        this.chatProvider = chatProvider;
        this.objectMapper = objectMapper;
        this.aiProcessingService = aiProcessingService;
        this.usageService = usageService;
    }

    public AiStatusDto status(Long fileId, String userId) {
        fileService.findOwnedForAi(fileId, userId);
        FileAiProcessing row = processingRepository.findById(fileId).orElse(null);
        if (row == null) {
            aiProcessingService.ensurePending(fileId);
            aiProcessingService.processAsync(fileId);
            row = processingRepository.findById(fileId).orElse(null);
        }
        return row == null ? new AiStatusDto(AiProcessingService.PENDING, null, null, null)
                : new AiStatusDto(row.getStatus(), row.getError(), row.getSummary(), row.getProcessedAt());
    }

    @Transactional
    public ChatResponse chat(Long fileId, String userId, String message) {
        FileEntity file = fileService.findOwnedForAi(fileId, userId);
        FileAiProcessing processing = processingRepository.findById(fileId)
                .orElseThrow(() -> new ApiException("File is not ready for chat.", HttpStatus.CONFLICT));
        if (!AiProcessingService.COMPLETED.equals(processing.getStatus())) {
            throw new ApiException("File AI processing is " + processing.getStatus().toLowerCase() + ".", HttpStatus.CONFLICT);
        }
        if (!embeddingProvider.isConfigured() || !chatProvider.isConfigured()) {
            throw new ApiException("AI chat is not configured.", HttpStatus.SERVICE_UNAVAILABLE);
        }
        if (usageService != null) usageService.consumeAiQuery(userId);
        float[] query = embeddingProvider.embed(message);
        List<FileAiChunk> candidates = chunkRepository.findByFileIdAndEmbeddingJsonIsNotNullOrderByChunkIndexAsc(fileId);
        List<ChunkMatch> matches = candidates.stream()
                .map(chunk -> toMatch(chunk, query))
                .filter(match -> match.score() >= 0.20)
                .sorted(Comparator.comparingDouble(ChunkMatch::score).reversed())
                .limit(5)
                .toList();
        if (matches.isEmpty()) throw new ApiException("No searchable content is available for this file.", HttpStatus.CONFLICT);
        String context = matches.stream()
                .map(m -> "[Source " + m.chunk().getChunkIndex() + "] " + m.chunk().getContent())
                .reduce("", (a, b) -> a + "\n\n" + b);
        String answer = chatProvider.complete(List.of(
                new ChatProvider.ChatMessage("system", "Answer only from the supplied file context. If the answer is not present, say so. Cite sources as [Source N]."),
                new ChatProvider.ChatMessage("user", "File: " + file.getOriginalFileName() + "\nContext:\n" + context + "\n\nQuestion: " + message)));
        saveMessage(fileId, userId, "user", message);
        saveMessage(fileId, userId, "assistant", answer);
        List<AiCitationDto> citations = matches.stream()
                .map(m -> new AiCitationDto(m.chunk().getChunkIndex(), m.chunk().getSourceMetadata(), excerpt(m.chunk().getContent()), m.score()))
                .toList();
        return new ChatResponse(answer, citations);
    }

    private void saveMessage(Long fileId, String userId, String role, String content) {
        FileAiChatMessage row = new FileAiChatMessage();
        row.setFileId(fileId); row.setUserId(userId); row.setRole(role); row.setContent(content);
        messageRepository.save(row);
    }

    private String excerpt(String value) { return value.length() > 240 ? value.substring(0, 240) + "…" : value; }

    private ChunkMatch toMatch(FileAiChunk chunk, float[] queryEmbedding) {
        try {
            float[] chunkEmbedding = objectMapper.readValue(chunk.getEmbeddingJson(), float[].class);
            return new ChunkMatch(chunk, cosineSimilarity(queryEmbedding, chunkEmbedding));
        } catch (Exception e) {
            return new ChunkMatch(chunk, 0.0);
        }
    }

    private double cosineSimilarity(float[] left, float[] right) {
        int length = Math.min(left.length, right.length);
        if (length == 0) return 0.0;
        double dot = 0.0;
        double leftNorm = 0.0;
        double rightNorm = 0.0;
        for (int i = 0; i < length; i++) {
            double l = left[i];
            double r = right[i];
            dot += l * r;
            leftNorm += l * l;
            rightNorm += r * r;
        }
        if (leftNorm == 0.0 || rightNorm == 0.0) return 0.0;
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }

    private record ChunkMatch(FileAiChunk chunk, double score) {}
}
