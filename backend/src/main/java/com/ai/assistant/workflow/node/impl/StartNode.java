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
import java.util.List;
import java.util.Map;

/**
 * 开始节点 - 工作流的入口点
 */
@Component
public class StartNode implements WorkflowNode {
    
    @Override
    public NodeOutput execute(NodeInput input, ExecutionContext context) throws NodeExecutionException {
        // Start节点直接传递输入数据
        return NodeOutput.builder()
                .data(input.getData())
                .status(NodeStatus.COMPLETED)
                .build();
    }
    
    @Override
    public void validateConfig(Map<String, Object> config) throws ValidationException {
        // Start节点配置验证（可选）
    }
    
    @Override
    public String getType() {
        return "start";
    }
    
    @Override
    public NodeMetadata getMetadata() {
        return NodeMetadata.builder()
                .type("start")
                .name("开始")
                .description("工作流的起始节点")
                .icon("start")
                .category("基础")
                .parameters(Arrays.asList(
                        NodeParameter.builder()
                                .name("fields")
                                .label("输入字段")
                                .type("array")
                                .description("定义工作流输入字段")
                                .required(false)
                                .build()
                ))
                .inputPorts(Arrays.asList())
                .outputPorts(Arrays.asList("output"))
                .build();
    }
}
