package com.ai.assistant.workflow.engine;

import com.ai.assistant.entity.WorkflowExecution;
import com.ai.assistant.entity.NodeExecution;

import java.util.List;
import java.util.Map;

/**
 * 工作流引擎接口
 */
public interface WorkflowEngine {
    /**
     * 执行工作流
     */
    WorkflowExecution execute(String workflowId, Map<String, Object> input);
    
    /**
     * 带回调的执行（用于流式返回）
     */
    void executeWithCallback(String workflowId, Map<String, Object> input, ExecutionCallback callback);
    
    /**
     * 异步执行工作流
     */
    WorkflowExecution executeAsync(String workflowId, Map<String, Object> input);
    
    /**
     * 取消执行
     */
    void cancel(String executionId);
    
    /**
     * 暂停执行
     */
    void pause(String executionId);
    
    /**
     * 恢复执行
     */
    void resume(String executionId);
    
    /**
     * 获取执行状态
     */
    WorkflowExecution getExecution(String executionId);
    
    /**
     * 获取节点执行列表
     */
    List<NodeExecution> getNodeExecutions(String executionId);
    
    /**
     * 执行回调接口
     */
    interface ExecutionCallback {
        void onNodeStart(String nodeId, String nodeName, String nodeType);
        void onNodeComplete(String nodeId, String nodeName, Map<String, Object> output);
        void onNodeError(String nodeId, String nodeName, String error);
        void onLLMToken(String nodeId, String token);
        void onComplete(Map<String, Object> finalOutput);
        void onError(String error);
    }
}
