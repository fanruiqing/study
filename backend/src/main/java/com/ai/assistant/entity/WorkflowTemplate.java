package com.ai.assistant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("workflow_templates")
public class WorkflowTemplate {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    
    private String name;
    private String description;
    private String category;
    private String definition;  // JSON: 工作流定义
    private String thumbnail;   // 缩略图路径
    private Boolean isPublic;
    private Long createdAt;
}
