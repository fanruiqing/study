package com.ai.assistant.workflow.model;

import lombok.Data;
import java.util.Map;

@Data
public class NodeDefinition {
    private String id;
    private String type;        // llm, knowledge_base, condition, loop, http, code, etc.
    private String name;
    private Map<String, Object> config;  // 节点特定配置
    private Position position;  // 画布位置
}
