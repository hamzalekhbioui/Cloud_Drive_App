package com.cloud.drive.dto;

import java.time.LocalDateTime;

public record AiStatusDto(String status, String error, String summary, LocalDateTime processedAt) {}
