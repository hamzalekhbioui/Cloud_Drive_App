package com.cloud.drive.dto;

public record AiCitationDto(int chunkIndex, String source, String excerpt, double score) {
    public int getChunkIndex() { return chunkIndex; }
    public String getSource() { return source; }
    public String getExcerpt() { return excerpt; }
    public double getScore() { return score; }
}
