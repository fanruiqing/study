package com.ai.assistant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("workflow_executions")
public class WorkflowExecution {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    
    private String workflowId;
    private String status;      // RUNNING, COMPLETED, FAILED, CANCELLED
    private String input;       // JSON: 输入参数
    private String output;      // JSON: 输出结果
    private String error;       // 错误信息
    private Long startTime;
    private Long endTime;
    private String executionLog; // JSON: 执行日志
}
