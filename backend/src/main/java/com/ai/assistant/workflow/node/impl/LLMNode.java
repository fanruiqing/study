package com.ai.assistant.workflow.node.impl;

import com.ai.assistant.service.ChatService;
import com.ai.assistant.workflow.exception.NodeExecutionException;
import com.ai.assistant.workflow.exception.ValidationException;
import com.ai.assistant.workflow.execution.ExecutionContext;
import com.ai.assistant.workflow.execution.NodeInput;
import com.ai.assistant.workflow.execution.NodeOutput;
import com.ai.assistant.workflow.execution.NodeStatus;
import com.ai.assistant.workflow.node.AbstractWorkflowNode;
import com.ai.assistant.workflow.node.NodeMetadata;
import com.ai.assistant.workflow.node.NodeParameter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class LLMNode extends AbstractWorkflowNode {
    
    private final ChatService chatService;
    
    @Override
    public NodeOutput execute(NodeInput input, ExecutionContext context) throws NodeExecutionException {
        try {
            String modelId = (String) input.getConfig().get("modelId");
            String prompt = (String) input.getConfig().get("prompt");
            
            // 替换提示词中的变量
            String resolvedPrompt = resolveVariables(prompt, context);
            
            // 调用 LLM（简化版本，实际需要更复杂的调用）
            String response = "LLM Response: " + resolvedPrompt; // TODO: 实际调用 chatService
            
            Map<String, Object> outputData = new HashMap<>();
            outputData.put("response", response);
            
            return NodeOutput.builder()
                    .data(outputData)
                    .status(NodeStatus.COMPLETED)
                    .build();
        } catch (Exception e) {
            throw new NodeExecutionException("LLM node execution failed", e);
        }
    }
    
    @Override
    public void validateConfig(Map<String, Object> config) throws ValidationException {
        validateRequiredConfig(config, "modelId", "prompt");
    }
    
    @Override
    public String getType() {
        return "llm";
    }
    
    @Override
    public NodeMetadata getMetadata() {
        return NodeMetadata.builder()
                .type("llm")
                .name("LLM节点")
                .description("调用大语言模型")
                .icon("brain")
                .category("AI")
                .parameters(Arrays.asList(
                        NodeParameter.builder()
                                .name("modelId")
                                .label("模型ID")
                                .type("select")
                                .description("选择要使用的模型")
                                .required(true)
                                .build(),
                        NodeParameter.builder()
                                .name("prompt")
                                .label("提示词")
                                .type("textarea")
                                .description("输入提示词，支持变量引用 {{variable}}")
                                .required(true)
                                .build()
                ))
                .inputPorts(Arrays.asList("input"))
                .outputPorts(Arrays.asList("output"))
                .build();
    }
}
