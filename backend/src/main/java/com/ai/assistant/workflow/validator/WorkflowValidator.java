package com.ai.assistant.workflow.validator;

import com.ai.assistant.workflow.model.EdgeDefinition;
import com.ai.assistant.workflow.model.NodeDefinition;
import com.ai.assistant.workflow.model.WorkflowDefinition;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class WorkflowValidator {
    
    public ValidationResult validate(WorkflowDefinition definition) {
        ValidationResult result = new ValidationResult();
        
        if (definition == null) {
            result.addError("Workflow definition is null");
            return result;
        }
        
        // 验证节点
        if (definition.getNodes() == null || definition.getNodes().isEmpty()) {
            result.addError("Workflow must have at least one node");
        }
        
        // 验证循环依赖
        if (hasCircularDependency(definition)) {
            result.addError("Workflow contains circular dependency");
        }
        
        // 验证连接有效性
        validateEdges(definition, result);
        
        return result;
    }
    
    private boolean hasCircularDependency(WorkflowDefinition definition) {
        Map<String, List<String>> graph = buildGraph(definition);
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();
        
        for (String node : graph.keySet()) {
            if (hasCycle(node, graph, visited, recursionStack)) {
                return true;
            }
        }
        
        return false;
    }
    
    private boolean hasCycle(String node, Map<String, List<String>> graph, 
                            Set<String> visited, Set<String> recursionStack) {
        if (recursionStack.contains(node)) {
            return true;
        }
        
        if (visited.contains(node)) {
            return false;
        }
        
        visited.add(node);
        recursionStack.add(node);
        
        List<String> neighbors = graph.getOrDefault(node, Collections.emptyList());
        for (String neighbor : neighbors) {
            if (hasCycle(neighbor, graph, visited, recursionStack)) {
                return true;
            }
        }
        
        recursionStack.remove(node);
        return false;
    }
    
    private Map<String, List<String>> buildGraph(WorkflowDefinition definition) {
        Map<String, List<String>> graph = new HashMap<>();
        
        if (definition.getEdges() != null) {
            for (EdgeDefinition edge : definition.getEdges()) {
                graph.computeIfAbsent(edge.getSource(), k -> new ArrayList<>())
                     .add(edge.getTarget());
            }
        }
        
        return graph;
    }
    
    private void validateEdges(WorkflowDefinition definition, ValidationResult result) {
        if (definition.getEdges() == null) {
            return;
        }
        
        Set<String> nodeIds = new HashSet<>();
        for (NodeDefinition node : definition.getNodes()) {
            nodeIds.add(node.getId());
        }
        
        for (EdgeDefinition edge : definition.getEdges()) {
            if (!nodeIds.contains(edge.getSource())) {
                result.addError("Edge source node not found: " + edge.getSource());
            }
            if (!nodeIds.contains(edge.getTarget())) {
                result.addError("Edge target node not found: " + edge.getTarget());
            }
        }
    }
    
    @Data
    public static class ValidationResult {
        private List<String> errors = new ArrayList<>();
        private List<String> warnings = new ArrayList<>();
        
        public void addError(String error) {
            errors.add(error);
        }
        
        public void addWarning(String warning) {
            warnings.add(warning);
        }
        
        public boolean isValid() {
            return errors.isEmpty();
        }
    }
}
