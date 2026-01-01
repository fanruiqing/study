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
@TableName("documents")
public class Document {
    
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    
    @TableField("knowledge_base_id")
    private String knowledgeBaseId;
    
    @TableField("file_name")
    private String fileName;
    
    @TableField("file_type")
    private String fileType;
    
    @TableField("file_size")
    private Long fileSize;
    
    @TableField("file_path")
    private String filePath;
    
    private String status;
    
    @TableField("error_message")
    private String errorMessage;
    
    @TableField("chunk_count")
    private Integer chunkCount;
    
    private String tags;  // JSON string
    
    private String category;
    
    @TableField("created_at")
    private Long createdAt;
    
    @TableField("updated_at")
    private Long updatedAt;
}
