package com.ai.assistant.workflow.registry;

import com.ai.assistant.workflow.node.NodeMetadata;
import com.ai.assistant.workflow.node.WorkflowNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Component
public class NodeRegistry {
    private final Map<String, WorkflowNode> nodes = new ConcurrentHashMap<>();
    
    /**
     * 注册节点
     */
    public void register(WorkflowNode node) {
        String type = node.getType();
        nodes.put(type, node);
        log.info("Registered workflow node: {}", type);
    }
    
    /**
     * 根据类型获取节点
     */
    public WorkflowNode getNode(String type) {
        return nodes.get(type);
    }
    
    /**
     * 获取所有节点类型
     */
    public List<NodeMetadata> getAllNodeTypes() {
        return nodes.values().stream()
                .map(WorkflowNode::getMetadata)
                .collect(Collectors.toList());
    }
    
    /**
     * 检查节点类型是否存在
     */
    public boolean hasNode(String type) {
        return nodes.containsKey(type);
    }
}
