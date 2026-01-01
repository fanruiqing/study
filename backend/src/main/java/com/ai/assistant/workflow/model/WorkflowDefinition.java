package com.ai.assistant.workflow.model;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class WorkflowDefinition {
    private String id;
    private String name;
    private String description;
    private List<NodeDefinition> nodes;
    private List<EdgeDefinition> edges;
    private Map<String, Object> variables;  // 全局变量
    private WorkflowConfig config;
}
