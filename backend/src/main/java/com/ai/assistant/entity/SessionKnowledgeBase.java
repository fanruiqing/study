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
@TableName("session_knowledge_bases")
public class SessionKnowledgeBase {
    
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    
    @TableField("session_id")
    private String sessionId;
    
    @TableField("knowledge_base_id")
    private String knowledgeBaseId;
    
    @TableField("created_at")
    private Long createdAt;
}
