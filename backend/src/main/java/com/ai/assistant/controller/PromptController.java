package com.ai.assistant.controller;

import com.ai.assistant.dto.PromptTemplateDTO;
import com.ai.assistant.service.PromptService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/prompts")
@RequiredArgsConstructor
public class PromptController {
    
    private final PromptService promptService;
    
    @GetMapping
    public ResponseEntity<List<PromptTemplateDTO>> getAllTemplates() {
        return ResponseEntity.ok(promptService.getAllTemplates());
    }
    
    @GetMapping("/grouped")
    public ResponseEntity<Map<String, List<PromptTemplateDTO>>> getTemplatesGrouped() {
        return ResponseEntity.ok(promptService.getTemplatesGroupedByCategory());
    }
    
    @GetMapping("/categories")
    public ResponseEntity<List<String>> getCategories() {
        return ResponseEntity.ok(promptService.getAllCategories());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<PromptTemplateDTO> getTemplate(@PathVariable String id) {
        return ResponseEntity.ok(promptService.getTemplate(id));
    }
    
    @PostMapping
    public ResponseEntity<PromptTemplateDTO> createTemplate(@RequestBody PromptTemplateDTO dto) {
        return ResponseEntity.ok(promptService.createTemplate(dto));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<PromptTemplateDTO> updateTemplate(
            @PathVariable String id,
            @RequestBody PromptTemplateDTO dto) {
        return ResponseEntity.ok(promptService.updateTemplate(id, dto));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable String id) {
        promptService.deleteTemplate(id);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<PromptTemplateDTO>> searchTemplates(@RequestParam String query) {
        return ResponseEntity.ok(promptService.searchTemplates(query));
    }
}
