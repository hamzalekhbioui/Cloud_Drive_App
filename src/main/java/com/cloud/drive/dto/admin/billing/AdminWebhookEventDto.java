package com.cloud.drive.dto.admin.billing;

import java.time.LocalDateTime;

public class AdminWebhookEventDto {
    private Long id;
    private String stripeEventId;
    private String eventType;
    private String payload;
    private boolean processed;
    private String processingError;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;

    public Long getId() { return id; }
    public void setId(Long value) { this.id = value; }
    public String getStripeEventId() { return stripeEventId; }
    public void setStripeEventId(String value) { this.stripeEventId = value; }
    public String getEventType() { return eventType; }
    public void setEventType(String value) { this.eventType = value; }
    public String getPayload() { return payload; }
    public void setPayload(String value) { this.payload = value; }
    public boolean isProcessed() { return processed; }
    public void setProcessed(boolean value) { this.processed = value; }
    public String getProcessingError() { return processingError; }
    public void setProcessingError(String value) { this.processingError = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime value) { this.createdAt = value; }
    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime value) { this.processedAt = value; }
}
