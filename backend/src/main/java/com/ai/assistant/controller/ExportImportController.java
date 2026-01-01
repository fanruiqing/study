package com.ai.assistant.controller;

import com.ai.assistant.dto.ExportData;
import com.ai.assistant.service.ExportImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/data")
@RequiredArgsConstructor
public class ExportImportController {
    
    private final ExportImportService exportImportService;
    
    @GetMapping("/export")
    public ResponseEntity<ExportData> exportData() {
        return ResponseEntity.ok(exportImportService.exportAll());
    }
    
    @GetMapping("/export/json")
    public ResponseEntity<byte[]> exportJson() {
        String json = exportImportService.exportToJson();
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ai-assistant-export.json")
                .contentType(MediaType.APPLICATION_JSON)
                .body(bytes);
    }
    
    @GetMapping("/export/markdown")
    public ResponseEntity<byte[]> exportMarkdown() {
        String markdown = exportImportService.exportToMarkdown();
        byte[] bytes = markdown.getBytes(StandardCharsets.UTF_8);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ai-assistant-export.md")
                .contentType(MediaType.TEXT_MARKDOWN)
                .body(bytes);
    }
    
    @PostMapping("/import")
    public ResponseEntity<Void> importData(@RequestBody String json) {
        exportImportService.importFromJson(json);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/import/file")
    public ResponseEntity<Void> importFile(@RequestParam("file") MultipartFile file) {
        try {
            String json = new String(file.getBytes(), StandardCharsets.UTF_8);
            exportImportService.importFromJson(json);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to import file", e);
        }
    }
}
