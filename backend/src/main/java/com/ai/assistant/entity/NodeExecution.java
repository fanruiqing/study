package com.ai.assistant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("node_executions")
public class NodeExecution {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    
    private String executionId;
    private String nodeId;
    private String nodeName;
    private String nodeType;
    private String status;      // PENDING, RUNNING, COMPLETED, FAILED, SKIPPED
    private String input;       // JSON
    private String output;      // JSON
    private String error;
    private Long startTime;
    private Long endTime;
    private Integer retryCount;
}
