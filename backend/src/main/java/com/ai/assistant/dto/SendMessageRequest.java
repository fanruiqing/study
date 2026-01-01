package com.ai.assistant.dto;

import lombok.Data;

@Data
public class SendMessageRequest {
    private String sessionId;
    private String content;
    private String modelId;
}
