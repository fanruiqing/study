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

import java.util.Arrays;
import java.util.Map;

@Component
public class OutputNode implements WorkflowNode {
    
    @Override
    public NodeOutput execute(NodeInput input, ExecutionContext context) throws NodeExecutionException {
        // Output节点返回配置的输出字段
        return NodeOutput.builder()
                .data(input.getData())
                .status(NodeStatus.COMPLETED)
                .build();
    }
    
    @Override
    public void validateConfig(Map<String, Object> config) throws ValidationException {
        if (config == null || !config.containsKey("fields")) {
            throw new ValidationException("Output node requires 'fields' configuration");
        }
    }
    
    @Override
    public String getType() {
        return "output";
    }
    
    @Override
    public NodeMetadata getMetadata() {
        return NodeMetadata.builder()
                .type("output")
                .name("输出节点")
                .description("返回工作流输出结果")
                .icon("output")
                .category("基础")
                .parameters(Arrays.asList(
                        NodeParameter.builder()
                                .name("fields")
                                .label("输出字段")
                                .type("object")
                                .description("定义输出字段映射")
                                .required(true)
                                .build()
                ))
                .inputPorts(Arrays.asList("input"))
                .outputPorts(Arrays.asList())
                .build();
    }
}
