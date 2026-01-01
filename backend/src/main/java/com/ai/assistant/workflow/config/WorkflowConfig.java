package com.ai.assistant.workflow.config;

import com.ai.assistant.workflow.node.WorkflowNode;
import com.ai.assistant.workflow.registry.NodeRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class WorkflowConfig {
    
    private final NodeRegistry nodeRegistry;
    private final List<WorkflowNode> workflowNodes;
    
    @PostConstruct
    public void registerNodes() {
        for (WorkflowNode node : workflowNodes) {
            nodeRegistry.register(node);
            log.info("Registered workflow node: {}", node.getType());
        }
    }
}
