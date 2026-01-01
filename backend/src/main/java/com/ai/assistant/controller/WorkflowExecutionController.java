package com.ai.assistant.controller;

import com.ai.assistant.entity.NodeExecution;
import com.ai.assistant.entity.WorkflowExecution;
import com.ai.assistant.workflow.engine.WorkflowEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/executions")
@RequiredArgsConstructor
public class WorkflowExecutionController {
    
    private final WorkflowEngine workflowEngine;
    
    @PostMapping("/workflows/{workflowId}/execute")
    public ResponseEntity<WorkflowExecution> execute(
            @PathVariable String workflowId,
            @RequestBody Map<String, Object> input) {
        return ResponseEntity.ok(workflowEngine.execute(workflowId, input));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<WorkflowExecution> getExecution(@PathVariable String id) {
        return ResponseEntity.ok(workflowEngine.getExecution(id));
    }
    
    @GetMapping("/{id}/nodes")
    public ResponseEntity<List<NodeExecution>> getNodeExecutions(@PathVariable String id) {
        return ResponseEntity.ok(workflowEngine.getNodeExecutions(id));
    }
    
    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancel(@PathVariable String id) {
        workflowEngine.cancel(id);
        return ResponseEntity.ok().build();
    }
}
