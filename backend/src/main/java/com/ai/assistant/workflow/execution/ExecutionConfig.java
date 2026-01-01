package com.ai.assistant.workflow.execution;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExecutionConfig {
    private Integer maxExecutionTime;
    private Integer maxRetries;
    private Boolean enableParallel;
    private Integer parallelLimit;
}
