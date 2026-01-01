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
@TableName("prompt_templates")
public class PromptTemplate {
    
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    
    private String title;
    
    private String content;
    
    private String category;
    
    @TableField("created_at")
    private Long createdAt;
    
    @TableField("updated_at")
    private Long updatedAt;
}
