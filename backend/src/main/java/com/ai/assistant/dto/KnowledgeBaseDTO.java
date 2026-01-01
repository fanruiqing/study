package com.ai.assistant.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;

@Data
public class KnowledgeBaseDTO {
    
    @NotBlank(message = "知识库名称不能为空")
    private String name;
    
    private String description;
    
    private String embeddingModel = "text-embedding-ada-002";
    
    private String providerId;
    
    private String providerName;
    
    @Min(value = 100, message = "分块大小不能小于100")
    private Integer chunkSize = 500;
    
    @Min(value = 0, message = "分块重叠不能小于0")
    private Integer chunkOverlap = 50;
}
