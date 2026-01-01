package com.ai.assistant.controller;

import com.ai.assistant.dto.WorkflowDTO;
import com.ai.assistant.entity.NodeExecution;
import com.ai.assistant.entity.Workflow;
import com.ai.assistant.entity.WorkflowExecution;
import com.ai.assistant.service.WorkflowService;
import com.ai.assistant.workflow.engine.WorkflowEngine;
import com.ai.assistant.workflow.validator.WorkflowValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@RestController
@RequestMapping("/api/workflows")
@RequiredArgsConstructor
public class WorkflowController {
    
    private final WorkflowService workflowService;
    private final WorkflowEngine workflowEngine;
    private final ObjectMapper objectMapper;
    
    private final ExecutorService executor = Executors.newCachedThreadPool();
    
    @PostMapping
    public ResponseEntity<Workflow> create(@RequestBody WorkflowDTO dto) {
        return ResponseEntity.ok(workflowService.create(dto));
    }
    
    @GetMapping
    public ResponseEntity<List<Workflow>> list() {
        return ResponseEntity.ok(workflowService.list());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Workflow> getById(@PathVariable String id) {
        return ResponseEntity.ok(workflowService.getById(id));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Workflow> update(@PathVariable String id, @RequestBody WorkflowDTO dto) {
        return ResponseEntity.ok(workflowService.update(id, dto));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        workflowService.delete(id);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/{id}/publish")
    public ResponseEntity<Workflow> publish(@PathVariable String id) {
        return ResponseEntity.ok(workflowService.publish(id));
    }
    
    @PostMapping("/{id}/unpublish")
    public ResponseEntity<Void> unpublish(@PathVariable String id) {
        workflowService.unpublish(id);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/{id}/validate")
    public ResponseEntity<WorkflowValidator.ValidationResult> validate(@PathVariable String id) {
        return ResponseEntity.ok(workflowService.validate(id));
    }
    
    // ========== 工作流执行相关 API ==========
    
    /**
     * 执行工作流
     */
    @PostMapping("/{id}/execute")
    public ResponseEntity<WorkflowExecution> execute(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, Object> input) {
        return ResponseEntity.ok(workflowEngine.execute(id, input != null ? input : Map.of()));
    }
    
    /**
     * 获取执行记录
     */
    @GetMapping("/executions/{executionId}")
    public ResponseEntity<WorkflowExecution> getExecution(@PathVariable String executionId) {
        return ResponseEntity.ok(workflowEngine.getExecution(executionId));
    }
    
    /**
     * 获取节点执行记录
     */
    @GetMapping("/executions/{executionId}/nodes")
    public ResponseEntity<List<NodeExecution>> getNodeExecutions(@PathVariable String executionId) {
        return ResponseEntity.ok(workflowEngine.getNodeExecutions(executionId));
    }
    
    /**
     * 直接执行工作流定义（无需先保存）- 用于预览模式
     */
    @PostMapping("/execute-preview")
    public ResponseEntity<WorkflowExecution> executePreview(@RequestBody WorkflowExecuteRequest request) {
        // 先临时保存工作流
        WorkflowDTO dto = new WorkflowDTO();
        dto.setName("Preview_" + System.currentTimeMillis());
        dto.setDescription("Preview execution");
        dto.setDefinition(request.getDefinition());
        
        Workflow workflow = workflowService.create(dto);
        
        try {
            // 执行工作流
            WorkflowExecution execution = workflowEngine.execute(workflow.getId(), request.getInput());
            return ResponseEntity.ok(execution);
        } finally {
            // 清理临时工作流
            workflowService.delete(workflow.getId());
        }
    }
    
    /**
     * 流式执行工作流（SSE）- 实时返回每个节点的执行状态
     */
    @GetMapping(value = "/execute-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter executeStream(
            @RequestParam String definition,
            @RequestParam(required = false) String input) {
        
        SseEmitter emitter = new SseEmitter(300000L); // 5分钟超时
        
        executor.execute(() -> {
            Workflow workflow = null;
            try {
                // 解析输入
                Map<String, Object> inputMap = Map.of();
                if (input != null && !input.isEmpty()) {
                    inputMap = objectMapper.readValue(input, Map.class);
                }
                
                // 临时保存工作流
                WorkflowDTO dto = new WorkflowDTO();
                dto.setName("Stream_" + System.currentTimeMillis());
                dto.setDescription("Stream execution");
                dto.setDefinition(definition);
                workflow = workflowService.create(dto);
                
                // 发送开始事件
                sendEvent(emitter, "start", Map.of("workflowId", workflow.getId()));
                
                // 流式执行
                workflowEngine.executeWithCallback(workflow.getId(), inputMap, new WorkflowEngine.ExecutionCallback() {
                    @Override
                    public void onNodeStart(String nodeId, String nodeName, String nodeType) {
                        sendEvent(emitter, "node_start", Map.of(
                            "nodeId", nodeId,
                            "nodeName", nodeName,
                            "nodeType", nodeType
                        ));
                    }
                    
                    @Override
                    public void onNodeComplete(String nodeId, String nodeName, Map<String, Object> output) {
                        sendEvent(emitter, "node_complete", Map.of(
                            "nodeId", nodeId,
                            "nodeName", nodeName,
                            "output", output != null ? output : Map.of()
                        ));
                    }
                    
                    @Override
                    public void onNodeError(String nodeId, String nodeName, String error) {
                        sendEvent(emitter, "node_error", Map.of(
                            "nodeId", nodeId,
                            "nodeName", nodeName,
                            "error", error
                        ));
                    }
                    
                    @Override
                    public void onLLMToken(String nodeId, String token) {
                        sendEvent(emitter, "llm_token", Map.of(
                            "nodeId", nodeId,
                            "token", token
                        ));
                    }
                    
                    @Override
                    public void onComplete(Map<String, Object> finalOutput) {
                        sendEvent(emitter, "complete", Map.of(
                            "output", finalOutput != null ? finalOutput : Map.of()
                        ));
                    }
                    
                    @Override
                    public void onError(String error) {
                        sendEvent(emitter, "error", Map.of("error", error));
                    }
                });
                
                emitter.complete();
                
            } catch (Exception e) {
                log.error("Stream execution failed", e);
                sendEvent(emitter, "error", Map.of("error", e.getMessage()));
                emitter.completeWithError(e);
            } finally {
                // 清理临时工作流
                if (workflow != null) {
                    try {
                        workflowService.delete(workflow.getId());
                    } catch (Exception e) {
                        log.warn("Failed to delete temp workflow", e);
                    }
                }
            }
        });
        
        return emitter;
    }
    
    private void sendEvent(SseEmitter emitter, String eventType, Map<String, Object> data) {
        try {
            String json = objectMapper.writeValueAsString(data);
            emitter.send(SseEmitter.event()
                    .name(eventType)
                    .data(json));
        } catch (Exception e) {
            log.error("Failed to send SSE event", e);
        }
    }
    
    /**
     * 工作流执行请求
     */
    public static class WorkflowExecuteRequest {
        private String definition;
        private Map<String, Object> input;
        
        public String getDefinition() { return definition; }
        public void setDefinition(String definition) { this.definition = definition; }
        public Map<String, Object> getInput() { return input; }
        public void setInput(Map<String, Object> input) { this.input = input; }
    }
}
