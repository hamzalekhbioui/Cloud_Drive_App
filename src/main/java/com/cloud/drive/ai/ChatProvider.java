package com.cloud.drive.ai;

import java.util.List;

public interface ChatProvider {
    String complete(List<ChatMessage> messages);
    boolean isConfigured();

    record ChatMessage(String role, String content) {}
}
