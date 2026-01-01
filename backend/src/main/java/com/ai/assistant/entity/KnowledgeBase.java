package com.ai.assistant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("knowledge_bases")
public class KnowledgeBase {
    
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    
    private String name;
    
    private String description;
    
    @TableField("embedding_model")
    private String embeddingModel;
    
    @TableField("provider_id")
    private String providerId;
    
    @TableField("provider_name")
    private String providerName;
    
    @TableField("chunk_size")
    private Integer chunkSize;
    
    @TableField("chunk_overlap")
    private Integer chunkOverlap;
    
    @TableField("created_at")
    private Long createdAt;
    
    @TableField("updated_at")
    private Long updatedAt;
}
