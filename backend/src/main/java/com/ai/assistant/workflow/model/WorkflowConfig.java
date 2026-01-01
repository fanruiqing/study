package com.ai.assistant.workflow.model;

import lombok.Data;

@Data
public class WorkflowConfig {
    private Integer maxExecutionTime;  // 最大执行时间（秒）
    private Integer maxRetries;        // 最大重试次数
    private Boolean enableParallel;    // 是否启用并行执行
    private Integer parallelLimit;     // 并行度限制
}
