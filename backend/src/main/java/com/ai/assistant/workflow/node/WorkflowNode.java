package com.ai.assistant.workflow.node;

import com.ai.assistant.workflow.execution.ExecutionContext;
import com.ai.assistant.workflow.execution.NodeInput;
import com.ai.assistant.workflow.execution.NodeOutput;
import com.ai.assistant.workflow.exception.NodeExecutionException;
import com.ai.assistant.workflow.exception.ValidationException;

import java.util.Map;

/**
 * 工作流节点接口
 */
public interface WorkflowNode {
    /**
     * 执行节点
     */
    NodeOutput execute(NodeInput input, ExecutionContext context) throws NodeExecutionException;
    
    /**
     * 验证配置
     */
    void validateConfig(Map<String, Object> config) throws ValidationException;
    
    /**
     * 获取节点类型
     */
    String getType();
    
    /**
     * 获取节点元数据（用于前端展示）
     */
    NodeMetadata getMetadata();
}
