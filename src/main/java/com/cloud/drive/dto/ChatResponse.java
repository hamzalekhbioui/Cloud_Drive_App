package com.cloud.drive.dto;

import java.util.List;

public record ChatResponse(String answer, List<AiCitationDto> citations) {
    public String getAnswer() { return answer; }
    public List<AiCitationDto> getCitations() { return citations; }
}
