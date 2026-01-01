package com.ai.assistant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("messages")
public class Message {
    
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    
    @TableField("session_id")
    private String sessionId;
    
    private String role;
    
    private String content;
    
    private Long timestamp;
    
    @TableField("model_id")
    private String modelId;
    
    private Integer rating;
    
    private String metadata;
}
