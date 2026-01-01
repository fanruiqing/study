package com.ai.assistant.workflow.execution;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class ExecutionContext {
    private String executionId;
    private String workflowId;
    private Map<String, Object> variables;  // 变量存储
    private Map<String, Object> nodeOutputs; // 节点输出缓存
    private ExecutionConfig config;
    
    public ExecutionContext() {
        this.variables = new ConcurrentHashMap<>();
        this.nodeOutputs = new ConcurrentHashMap<>();
    }
    
    public void setVariable(String key, Object value) {
        variables.put(key, value);
    }
    
    public Object getVariable(String key) {
        return variables.get(key);
    }
    
    public Object getNodeOutput(String nodeId) {
        return nodeOutputs.get(nodeId);
    }
    
    public void setNodeOutput(String nodeId, Object output) {
        nodeOutputs.put(nodeId, output);
    }
}
