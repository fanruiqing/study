package com.ai.assistant.workflow.node.impl;

import com.ai.assistant.workflow.exception.NodeExecutionException;
import com.ai.assistant.workflow.exception.ValidationException;
import com.ai.assistant.workflow.execution.ExecutionContext;
import com.ai.assistant.workflow.execution.NodeInput;
import com.ai.assistant.workflow.execution.NodeOutput;
import com.ai.assistant.workflow.execution.NodeStatus;
import com.ai.assistant.workflow.node.AbstractWorkflowNode;
import com.ai.assistant.workflow.node.NodeMetadata;
import com.ai.assistant.workflow.node.NodeParameter;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Component
public class ConditionNode extends AbstractWorkflowNode {
    
    @Override
    public NodeOutput execute(NodeInput input, ExecutionContext context) throws NodeExecutionException {
        String condition = (String) input.getConfig().get("condition");
        boolean result = evaluateCondition(condition, context);
        
        Map<String, Object> outputData = new HashMap<>();
        outputData.put("result", result);
        outputData.put("branch", result ? "true" : "false");
        
        return NodeOutput.builder()
                .data(outputData)
                .status(NodeStatus.COMPLETED)
                .build();
    }
    
    private boolean evaluateCondition(String condition, ExecutionContext context) {
        // 简化的条件评估
        return true;
    }
    
    @Override
    public void validateConfig(Map<String, Object> config) throws ValidationException {
        validateRequiredConfig(config, "condition");
    }
    
    @Override
    public String getType() {
        return "condition";
    }
    
    @Override
    public NodeMetadata getMetadata() {
        return NodeMetadata.builder()
                .type("condition")
                .name("条件节点")
                .description("条件判断分支")
                .icon("fork")
                .category("逻辑")
                .parameters(Arrays.asList(
                        NodeParameter.builder()
                                .name("condition")
                                .label("条件表达式")
                                .type("string")
                                .required(true)
                                .build()
                ))
                .inputPorts(Arrays.asList("input"))
                .outputPorts(Arrays.asList("true", "false"))
                .build();
    }
}
