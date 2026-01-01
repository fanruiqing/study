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
@TableName("document_chunks")
public class DocumentChunk {
    
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    
    @TableField("document_id")
    private String documentId;
    
    private String content;
    
    @TableField("chunk_index")
    private Integer chunkIndex;
    
    @TableField("vector_id")
    private String vectorId;
    
    private String metadata;  // JSON string
    
    @TableField("created_at")
    private Long createdAt;
}
