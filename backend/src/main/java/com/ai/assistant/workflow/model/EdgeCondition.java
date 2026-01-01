package com.ai.assistant.workflow.model;

import lombok.Data;

@Data
public class EdgeCondition {
    private String expression;  // 条件表达式
    private String value;       // 条件值（如 "true", "false"）
}
