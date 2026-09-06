package com.cloud.drive.service;

import com.cloud.drive.ai.EmbeddingProvider;
import com.cloud.drive.ai.ChatProvider;
import com.cloud.drive.ai.FileAiQueuedEvent;
import com.cloud.drive.exception.ApiException;
import com.cloud.drive.model.FileAiChunk;
import com.cloud.drive.model.FileAiProcessing;
import com.cloud.drive.model.FileEntity;
import com.cloud.drive.repository.FileAiChunkRepository;
import com.cloud.drive.repository.FileAiProcessingRepository;
import com.cloud.drive.repository.FileRepository;
import com.cloud.drive.storage.StorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AiProcessingService {
    /*
     * File indexing is a separate asynchronous product operation and does not
     * consume the monthly interactive AI query allowance. Only chat requests
     * are counted by UsageService, so failed indexing cannot consume queries.
     */
    public static final String PENDING = "PENDING";
    public static final String PROCESSING = "PROCESSING";
    public static final String COMPLETED = "COMPLETED";
    public static final String ERROR = "ERROR";
    public static final String UNSUPPORTED = "UNSUPPORTED";
    private static final int MAX_BYTES = 50 * 1024 * 1024;
    private static final int CHUNK_SIZE = 1200;
    private static final int OVERLAP = 150;

    private final FileRepository fileRepository;
    private final FileAiProcessingRepository processingRepository;
    private final FileAiChunkRepository chunkRepository;
    private final StorageService storageService;
    private final EmbeddingProvider embeddingProvider;
    private final ChatProvider chatProvider;
    private final ObjectMapper objectMapper;

    public AiProcessingService(FileRepository fileRepository,
                               FileAiProcessingRepository processingRepository,
                               FileAiChunkRepository chunkRepository,
                               StorageService storageService,
                               EmbeddingProvider embeddingProvider,
                               ChatProvider chatProvider,
                               ObjectMapper objectMapper) {
        this.fileRepository = fileRepository;
        this.processingRepository = processingRepository;
        this.chunkRepository = chunkRepository;
        this.storageService = storageService;
        this.embeddingProvider = embeddingProvider;
        this.chatProvider = chatProvider;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void ensurePending(Long fileId) {
        FileAiProcessing row = processingRepository.findById(fileId).orElseGet(() -> new FileAiProcessing(fileId));
        row.setStatus(PENDING);
        row.setError(null);
        row.setUpdatedAt(LocalDateTime.now());
        processingRepository.save(row);
    }

    @Async
    @Transactional
    public void processAsync(Long fileId) {
        try {
            process(fileId);
        } catch (Exception e) {
            markError(fileId, safeMessage(e));
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void processAfterCommit(FileAiQueuedEvent event) {
        try {
            process(event.fileId());
        } catch (Exception e) {
            markError(event.fileId(), safeMessage(e));
        }
    }

    @Transactional
    public synchronized void process(Long fileId) {
        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ApiException("File not found", org.springframework.http.HttpStatus.NOT_FOUND));
        FileAiProcessing row = processingRepository.findById(fileId).orElseGet(() -> new FileAiProcessing(fileId));
        row.setStatus(PROCESSING);
        row.setError(null);
        row.setUpdatedAt(LocalDateTime.now());
        processingRepository.save(row);

        if (!isSupported(file.getType())) {
            row.setStatus(UNSUPPORTED);
            row.setError("AI chat supports PDF and DOCX files only.");
            row.setUpdatedAt(LocalDateTime.now());
            processingRepository.save(row);
            return;
        }
        if (!embeddingProvider.isConfigured() || !chatProvider.isConfigured()) {
            throw new ApiException("AI processing is not configured. Configure Azure OpenAI before processing files.",
                    org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE);
        }

        byte[] bytes = readBlob(file);
        String text;
        try {
            text = extract(file.getType(), bytes).trim();
        } catch (Exception e) {
            throw new ApiException("Unable to extract text from this file.", org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if (text.isBlank()) throw new ApiException("No readable text was found in this file.", org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY);
        List<String> chunks = chunk(text);
        chunkRepository.deleteByFileId(fileId);
        for (int i = 0; i < chunks.size(); i++) {
            FileAiChunk chunk = new FileAiChunk();
            chunk.setFileId(fileId);
            chunk.setChunkIndex(i);
            chunk.setContent(chunks.get(i));
            chunk.setSourceMetadata("chunk:" + i);
            try {
                String embedding = objectMapper.writeValueAsString(embeddingProvider.embed(chunks.get(i)));
                chunk.setEmbeddingJson(embedding);
                chunkRepository.save(chunk);
            } catch (Exception e) {
                throw new ApiException("Failed to create an embedding for the file.", org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE);
            }
        }
        String summary = chatProvider.complete(List.of(
                new ChatProvider.ChatMessage("system", "Summarize the supplied document in five concise sentences."),
                new ChatProvider.ChatMessage("user", text.substring(0, Math.min(text.length(), 12000)))));
        row.setSummary(summary);
        row.setStatus(COMPLETED);
        row.setProcessedAt(LocalDateTime.now());
        row.setUpdatedAt(LocalDateTime.now());
        processingRepository.save(row);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markError(Long fileId, String message) {
        FileAiProcessing row = processingRepository.findById(fileId).orElseGet(() -> new FileAiProcessing(fileId));
        row.setStatus(ERROR);
        row.setError(message.length() > 1000 ? message.substring(0, 1000) : message);
        row.setUpdatedAt(LocalDateTime.now());
        processingRepository.save(row);
    }

    @Transactional
    public void deleteForFile(Long fileId) {
        chunkRepository.deleteByFileId(fileId);
        processingRepository.deleteById(fileId);
    }

    private byte[] readBlob(FileEntity file) {
        if (file.getSize() != null && file.getSize() > MAX_BYTES) {
            throw new ApiException("AI processing is limited to files of 50 MB or less.", org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE);
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        storageService.streamTo(file.getBlobFileName(), null, out);
        byte[] bytes = out.toByteArray();
        if (bytes.length > MAX_BYTES) throw new ApiException("AI processing is limited to files of 50 MB or less.", org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE);
        return bytes;
    }

    private String extract(String type, byte[] bytes) throws Exception {
        if ("application/pdf".equals(type)) {
            try (var document = Loader.loadPDF(bytes)) {
                return new PDFTextStripper().getText(document);
            }
        }
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes));
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }

    private List<String> chunk(String text) {
        List<String> result = new ArrayList<>();
        for (int start = 0; start < text.length(); ) {
            int end = Math.min(text.length(), start + CHUNK_SIZE);
            result.add(text.substring(start, end));
            if (end == text.length()) break;
            start = Math.max(start + 1, end - OVERLAP);
        }
        return result;
    }

    private boolean isSupported(String type) {
        return "application/pdf".equals(type)
                || "application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(type);
    }

    private String safeMessage(Exception e) {
        String message = e.getMessage();
        return message == null || message.isBlank() ? "AI processing failed." : message;
    }
}
