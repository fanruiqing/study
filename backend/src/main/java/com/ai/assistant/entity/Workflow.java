package com.ai.assistant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("workflows")
public class Workflow {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    
    private String name;
    private String description;
    private String definition;  // JSON: 完整的工作流定义
    private String status;      // DRAFT, PUBLISHED, ARCHIVED
    private String version;
    private String apiEndpoint; // 发布后的 API 路径
    private Long createdAt;
    private Long updatedAt;
    private String createdBy;
}
