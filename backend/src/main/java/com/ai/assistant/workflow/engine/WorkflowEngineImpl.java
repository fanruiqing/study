package com.ai.assistant.workflow.engine;

import com.ai.assistant.entity.NodeExecution;
import com.ai.assistant.entity.Workflow;
import com.ai.assistant.entity.WorkflowExecution;
import com.ai.assistant.mapper.NodeExecutionMapper;
import com.ai.assistant.mapper.WorkflowExecutionMapper;
import com.ai.assistant.mapper.WorkflowMapper;
import com.ai.assistant.workflow.exception.NodeExecutionException;
import com.ai.assistant.workflow.execution.ExecutionContext;
import com.ai.assistant.workflow.execution.NodeInput;
import com.ai.assistant.workflow.execution.NodeOutput;
import com.ai.assistant.workflow.execution.NodeStatus;
import com.ai.assistant.workflow.model.EdgeDefinition;
import com.ai.assistant.workflow.model.NodeDefinition;
import com.ai.assistant.workflow.model.WorkflowDefinition;
import com.ai.assistant.workflow.node.WorkflowNode;
import com.ai.assistant.workflow.registry.NodeRegistry;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowEngineImpl implements WorkflowEngine {
    
    private final WorkflowMapper workflowMapper;
    private final WorkflowExecutionMapper executionMapper;
    private final NodeExecutionMapper nodeExecutionMapper;
    private final NodeRegistry nodeRegistry;
    private final ObjectMapper objectMapper;
    
    @Override
    public WorkflowExecution execute(String workflowId, Map<String, Object> input) {
        // 加载工作流定义
        Workflow workflow = workflowMapper.selectById(workflowId);
        if (workflow == null) {
            throw new RuntimeException("Workflow not found: " + workflowId);
        }
        
        WorkflowDefinition definition;
        try {
            definition = objectMapper.readValue(workflow.getDefinition(), WorkflowDefinition.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse workflow definition", e);
        }
        
        // 创建执行记录
        WorkflowExecution execution = new WorkflowExecution();
        execution.setWorkflowId(workflowId);
        execution.setStatus("RUNNING");
        execution.setInput(toJson(input));
        execution.setStartTime(System.currentTimeMillis());
        executionMapper.insert(execution);
        
        // 创建执行上下文
        ExecutionContext context = new ExecutionContext();
        context.setExecutionId(execution.getId());
        context.setWorkflowId(workflowId);
        context.setVariables(new HashMap<>(input));
        
        try {
            // 执行工作流
            Map<String, Object> result = executeWorkflow(definition, context);
            
            // 更新执行结果
            execution.setStatus("COMPLETED");
            execution.setOutput(toJson(result));
            execution.setEndTime(System.currentTimeMillis());
            executionMapper.updateById(execution);
            
        } catch (Exception e) {
            log.error("Workflow execution failed", e);
            execution.setStatus("FAILED");
            execution.setError(e.getMessage());
            execution.setEndTime(System.currentTimeMillis());
            executionMapper.updateById(execution);
        }
        
        return execution;
    }
    
    private Map<String, Object> executeWorkflow(WorkflowDefinition definition, ExecutionContext context) {
        // 拓扑排序获取执行顺序
        List<NodeDefinition> executionOrder = topologicalSort(definition);
        
        // 按顺序执行节点
        for (NodeDefinition nodeDef : executionOrder) {
            executeNode(nodeDef, context);
        }
        
        // 返回输出节点的结果
        return context.getVariables();
    }
    
    private void executeNode(NodeDefinition nodeDef, ExecutionContext context) {
        WorkflowNode node = nodeRegistry.getNode(nodeDef.getType());
        if (node == null) {
            throw new RuntimeException("Node type not found: " + nodeDef.getType());
        }
        
        // 创建节点执行记录
        NodeExecution nodeExecution = new NodeExecution();
        nodeExecution.setExecutionId(context.getExecutionId());
        nodeExecution.setNodeId(nodeDef.getId());
        nodeExecution.setNodeName(nodeDef.getName());
        nodeExecution.setNodeType(nodeDef.getType());
        nodeExecution.setStatus("RUNNING");
        nodeExecution.setStartTime(System.currentTimeMillis());
        nodeExecutionMapper.insert(nodeExecution);
        
        try {
            // 准备输入
            NodeInput input = NodeInput.builder()
                    .data(context.getVariables())
                    .config(nodeDef.getConfig())
                    .build();
            
            // 执行节点
            NodeOutput output = node.execute(input, context);
            
            // 保存输出到上下文
            context.setNodeOutput(nodeDef.getId(), output.getData());
            if (output.getData() != null) {
                context.getVariables().putAll(output.getData());
            }
            
            // 更新节点执行记录
            nodeExecution.setStatus("COMPLETED");
            nodeExecution.setOutput(toJson(output.getData()));
            nodeExecution.setEndTime(System.currentTimeMillis());
            nodeExecutionMapper.updateById(nodeExecution);
            
        } catch (Exception e) {
            log.error("Node execution failed: {}", nodeDef.getId(), e);
            nodeExecution.setStatus("FAILED");
            nodeExecution.setError(e.getMessage());
            nodeExecution.setEndTime(System.currentTimeMillis());
            nodeExecutionMapper.updateById(nodeExecution);
            throw new RuntimeException("Node execution failed", e);
        }
    }
    
    private List<NodeDefinition> topologicalSort(WorkflowDefinition definition) {
        Map<String, NodeDefinition> nodeMap = definition.getNodes().stream()
                .collect(Collectors.toMap(NodeDefinition::getId, n -> n));
        
        Map<String, List<String>> graph = new HashMap<>();
        Map<String, Integer> inDegree = new HashMap<>();
        
        // 初始化
        for (NodeDefinition node : definition.getNodes()) {
            graph.put(node.getId(), new ArrayList<>());
            inDegree.put(node.getId(), 0);
        }
        
        // 构建图
        if (definition.getEdges() != null) {
            for (EdgeDefinition edge : definition.getEdges()) {
                graph.get(edge.getSource()).add(edge.getTarget());
                inDegree.put(edge.getTarget(), inDegree.get(edge.getTarget()) + 1);
            }
        }
        
        // 拓扑排序
        Queue<String> queue = new LinkedList<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.offer(entry.getKey());
            }
        }
        
        List<NodeDefinition> result = new ArrayList<>();
        while (!queue.isEmpty()) {
            String nodeId = queue.poll();
            result.add(nodeMap.get(nodeId));
            
            for (String neighbor : graph.get(nodeId)) {
                inDegree.put(neighbor, inDegree.get(neighbor) - 1);
                if (inDegree.get(neighbor) == 0) {
                    queue.offer(neighbor);
                }
            }
        }
        
        return result;
    }
    
    @Override
    public WorkflowExecution executeAsync(String workflowId, Map<String, Object> input) {
        // TODO: 实现异步执行
        return execute(workflowId, input);
    }
    
    @Override
    public void executeWithCallback(String workflowId, Map<String, Object> input, ExecutionCallback callback) {
        // 加载工作流定义
        Workflow workflow = workflowMapper.selectById(workflowId);
        if (workflow == null) {
            callback.onError("Workflow not found: " + workflowId);
            return;
        }
        
        WorkflowDefinition definition;
        try {
            definition = objectMapper.readValue(workflow.getDefinition(), WorkflowDefinition.class);
        } catch (Exception e) {
            callback.onError("Failed to parse workflow definition: " + e.getMessage());
            return;
        }
        
        // 创建执行上下文
        ExecutionContext context = new ExecutionContext();
        context.setWorkflowId(workflowId);
        context.setVariables(new HashMap<>(input));
        
        try {
            // 拓扑排序获取执行顺序
            List<NodeDefinition> executionOrder = topologicalSort(definition);
            
            // 按顺序执行节点
            for (NodeDefinition nodeDef : executionOrder) {
                executeNodeWithCallback(nodeDef, context, callback);
            }
            
            // 执行完成
            callback.onComplete(context.getVariables());
            
        } catch (Exception e) {
            log.error("Workflow execution failed", e);
            callback.onError(e.getMessage());
        }
    }
    
    private void executeNodeWithCallback(NodeDefinition nodeDef, ExecutionContext context, ExecutionCallback callback) {
        WorkflowNode node = nodeRegistry.getNode(nodeDef.getType());
        if (node == null) {
            callback.onNodeError(nodeDef.getId(), nodeDef.getName(), "Node type not found: " + nodeDef.getType());
            throw new RuntimeException("Node type not found: " + nodeDef.getType());
        }
        
        // 通知节点开始
        callback.onNodeStart(nodeDef.getId(), nodeDef.getName(), nodeDef.getType());
        
        try {
            // 准备输入
            NodeInput input = NodeInput.builder()
                    .data(context.getVariables())
                    .config(nodeDef.getConfig())
                    .build();
            
            // 如果是LLM节点，使用流式执行
            if ("llm".equals(nodeDef.getType())) {
                executeLLMNodeWithStreaming(nodeDef, input, context, callback);
            } else {
                // 普通节点执行
                NodeOutput output = node.execute(input, context);
                
                // 保存输出到上下文
                context.setNodeOutput(nodeDef.getId(), output.getData());
                if (output.getData() != null) {
                    context.getVariables().putAll(output.getData());
                }
                
                // 通知节点完成
                callback.onNodeComplete(nodeDef.getId(), nodeDef.getName(), output.getData());
            }
            
        } catch (Exception e) {
            log.error("Node execution failed: {}", nodeDef.getId(), e);
            callback.onNodeError(nodeDef.getId(), nodeDef.getName(), e.getMessage());
            throw new RuntimeException("Node execution failed", e);
        }
    }
    
    private void executeLLMNodeWithStreaming(NodeDefinition nodeDef, NodeInput input, ExecutionContext context, ExecutionCallback callback) {
        try {
            Map<String, Object> config = nodeDef.getConfig();
            String systemPrompt = (String) config.getOrDefault("systemPrompt", "");
            String userPromptTemplate = (String) config.getOrDefault("userPromptTemplate", "");
            String modelId = (String) config.getOrDefault("modelId", "gpt-3.5-turbo");
            
            // 替换变量
            String resolvedPrompt = resolveVariables(userPromptTemplate, context.getVariables());
            
            // 构建完整提示
            StringBuilder fullResponse = new StringBuilder();
            
            // 模拟流式输出（实际应该调用真实的LLM服务）
            // TODO: 集成真实的流式LLM调用
            String mockResponse = "这是对 \"" + resolvedPrompt + "\" 的回答。\n\n";
            mockResponse += "根据您的请求，我来为您分析：\n";
            mockResponse += "1. 首先，我们需要理解问题的核心...\n";
            mockResponse += "2. 然后，基于上下文信息进行分析...\n";
            mockResponse += "3. 最后，给出综合性的建议。\n\n";
            mockResponse += "希望这个回答对您有帮助！";
            
            // 模拟流式输出
            for (int i = 0; i < mockResponse.length(); i++) {
                String token = String.valueOf(mockResponse.charAt(i));
                fullResponse.append(token);
                callback.onLLMToken(nodeDef.getId(), token);
                
                // 模拟延迟
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            // 保存输出
            Map<String, Object> outputData = new HashMap<>();
            outputData.put("response", fullResponse.toString());
            
            context.setNodeOutput(nodeDef.getId(), outputData);
            context.getVariables().putAll(outputData);
            
            callback.onNodeComplete(nodeDef.getId(), nodeDef.getName(), outputData);
            
        } catch (Exception e) {
            throw new RuntimeException("LLM execution failed", e);
        }
    }
    
    private String resolveVariables(String template, Map<String, Object> variables) {
        if (template == null) return "";
        String result = template;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String value = entry.getValue() != null ? entry.getValue().toString() : "";
            result = result.replace(placeholder, value);
        }
        return result;
    }
    
    @Override
    public void cancel(String executionId) {
        // TODO: 实现取消逻辑
    }
    
    @Override
    public void pause(String executionId) {
        // TODO: 实现暂停逻辑
    }
    
    @Override
    public void resume(String executionId) {
        // TODO: 实现恢复逻辑
    }
    
    @Override
    public WorkflowExecution getExecution(String executionId) {
        return executionMapper.selectById(executionId);
    }
    
    @Override
    public List<NodeExecution> getNodeExecutions(String executionId) {
        QueryWrapper<NodeExecution> query = new QueryWrapper<>();
        query.eq("execution_id", executionId);
        return nodeExecutionMapper.selectList(query);
    }
    
    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }
}
