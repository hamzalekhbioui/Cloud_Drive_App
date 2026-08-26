package com.cloud.drive.dto;

import java.util.List;

public record ChatResponse(String answer, List<AiCitationDto> citations) {}
