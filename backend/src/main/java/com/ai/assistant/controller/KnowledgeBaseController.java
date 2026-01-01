package com.ai.assistant.controller;

import com.ai.assistant.dto.KnowledgeBaseDTO;
import com.ai.assistant.entity.KnowledgeBase;
import com.ai.assistant.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/knowledge-bases")
@RequiredArgsConstructor
public class KnowledgeBaseController {
    
    private final KnowledgeBaseService knowledgeBaseService;
    
    @PostMapping
    public ResponseEntity<KnowledgeBase> create(@Validated @RequestBody KnowledgeBaseDTO dto) {
        KnowledgeBase knowledgeBase = knowledgeBaseService.create(dto);
        return ResponseEntity.ok(knowledgeBase);
    }
    
    @GetMapping
    public ResponseEntity<List<KnowledgeBase>> list() {
        List<KnowledgeBase> knowledgeBases = knowledgeBaseService.list();
        return ResponseEntity.ok(knowledgeBases);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<KnowledgeBase> getById(@PathVariable String id) {
        KnowledgeBase knowledgeBase = knowledgeBaseService.getById(id);
        if (knowledgeBase == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(knowledgeBase);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<KnowledgeBase> update(
            @PathVariable String id,
            @Validated @RequestBody KnowledgeBaseDTO dto) {
        KnowledgeBase knowledgeBase = knowledgeBaseService.update(id, dto);
        return ResponseEntity.ok(knowledgeBase);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        knowledgeBaseService.delete(id);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/{id}/associate-session")
    public ResponseEntity<Void> associateWithSession(
            @PathVariable String id,
            @RequestBody Map<String, Object> request) {
        String sessionId = (String) request.get("sessionId");
        List<String> knowledgeBaseIds = (List<String>) request.get("knowledgeBaseIds");
        
        knowledgeBaseService.associateWithSession(sessionId, knowledgeBaseIds);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<List<KnowledgeBase>> getBySessionId(@PathVariable String sessionId) {
        List<KnowledgeBase> knowledgeBases = knowledgeBaseService.getBySessionId(sessionId);
        return ResponseEntity.ok(knowledgeBases);
    }
    
    @PostMapping("/update-providers")
    public ResponseEntity<Map<String, Object>> updateProviders() {
        int updated = knowledgeBaseService.updateAllProvidersInfo();
        return ResponseEntity.ok(Map.of(
            "success", true,
            "updated", updated,
            "message", "已更新 " + updated + " 个知识库的提供商信息"
        ));
    }
}
