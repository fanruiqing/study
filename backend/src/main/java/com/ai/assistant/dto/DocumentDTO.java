package com.ai.assistant.dto;

import lombok.Data;
import lombok.Builder;

import java.util.List;

@Data
@Builder
public class DocumentDTO {
    private String id;
    private String knowledgeBaseId;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private String status;
    private String errorMessage;
    private Integer chunkCount;
    private List<String> tags;
    private String category;
    private Long createdAt;
    private Long updatedAt;
}
