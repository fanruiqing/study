package com.ai.assistant.workflow.node.impl;

import com.ai.assistant.workflow.exception.NodeExecutionException;
import com.ai.assistant.workflow.exception.ValidationException;
import com.ai.assistant.workflow.execution.ExecutionContext;
import com.ai.assistant.workflow.execution.NodeInput;
import com.ai.assistant.workflow.execution.NodeOutput;
import com.ai.assistant.workflow.execution.NodeStatus;
import com.ai.assistant.workflow.node.NodeMetadata;
import com.ai.assistant.workflow.node.NodeParameter;
import com.ai.assistant.workflow.node.WorkflowNode;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class InputNode implements WorkflowNode {
    
    @Override
    public NodeOutput execute(NodeInput input, ExecutionContext context) throws NodeExecutionException {
        // Input节点直接返回输入数据
        return NodeOutput.builder()
                .data(input.getData())
                .status(NodeStatus.COMPLETED)
                .build();
    }
    
    @Override
    public void validateConfig(Map<String, Object> config) throws ValidationException {
        // Input节点配置验证
        if (config == null || !config.containsKey("fields")) {
            throw new ValidationException("Input node requires 'fields' configuration");
        }
    }
    
    @Override
    public String getType() {
        return "input";
    }
    
    @Override
    public NodeMetadata getMetadata() {
        return NodeMetadata.builder()
                .type("input")
                .name("输入节点")
                .description("接收工作流输入参数")
                .icon("input")
                .category("基础")
                .parameters(Arrays.asList(
                        NodeParameter.builder()
                                .name("fields")
                                .label("输入字段")
                                .type("array")
                                .description("定义输入字段列表")
                                .required(true)
                                .build()
                ))
                .inputPorts(Arrays.asList())
                .outputPorts(Arrays.asList("output"))
                .build();
    }
}
