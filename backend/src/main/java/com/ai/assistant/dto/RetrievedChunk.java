package com.ai.assistant.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class RetrievedChunk {
    private String chunkId;
    private String documentId;
    private String documentName;
    private String content;
    private float relevanceScore;
    private Map<String, Object> metadata;
}
