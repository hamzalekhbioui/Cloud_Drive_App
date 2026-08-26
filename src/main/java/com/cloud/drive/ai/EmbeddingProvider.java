package com.cloud.drive.ai;

public interface EmbeddingProvider {
    float[] embed(String text);
    boolean isConfigured();
}
