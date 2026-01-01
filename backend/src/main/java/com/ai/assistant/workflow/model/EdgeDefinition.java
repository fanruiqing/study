package com.ai.assistant.workflow.model;

import lombok.Data;

@Data
public class EdgeDefinition {
    private String id;
    private String source;      // 源节点 ID
    private String target;      // 目标节点 ID
    private String sourceHandle; // 源节点的输出端口
    private String targetHandle; // 目标节点的输入端口
    private EdgeCondition condition; // 可选：条件判断
}
