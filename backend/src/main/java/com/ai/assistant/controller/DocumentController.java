package com.ai.assistant.controller;

import com.ai.assistant.dto.DocumentDTO;
import com.ai.assistant.entity.Document;
import com.ai.assistant.entity.DocumentChunk;
import com.ai.assistant.mapper.DocumentChunkMapper;
import com.ai.assistant.service.DocumentService;
import com.ai.assistant.service.DocumentVectorizationService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {
    
    private final DocumentService documentService;
    private final DocumentVectorizationService documentVectorizationService;
    private final DocumentChunkMapper documentChunkMapper;
    
    @PostMapping("/upload")
    public ResponseEntity<DocumentDTO> upload(
            @RequestParam String knowledgeBaseId,
            @RequestParam MultipartFile file,
            @RequestParam(required = false) String tags,
            @RequestParam(required = false) String category) throws IOException {
        
        List<String> tagList = null;
        if (tags != null && !tags.isEmpty()) {
            tagList = Arrays.asList(tags.split(","));
        }
        
        Document document = documentService.upload(knowledgeBaseId, file, tagList, category);
        
        // 异步处理文档
        documentVectorizationService.processDocument(document.getId());
        
        return ResponseEntity.ok(documentService.toDTO(document));
    }
    
    @GetMapping
    public ResponseEntity<List<DocumentDTO>> list(@RequestParam String knowledgeBaseId) {
        List<Document> documents = documentService.listByKnowledgeBase(knowledgeBaseId);
        List<DocumentDTO> dtos = documents.stream()
                .map(documentService::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<DocumentDTO> getById(@PathVariable String id) {
        Document document = documentService.getById(id);
        if (document == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(documentService.toDTO(document));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        documentService.delete(id);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/{id}/status")
    public ResponseEntity<DocumentDTO> getStatus(@PathVariable String id) {
        Document document = documentService.getById(id);
        if (document == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(documentService.toDTO(document));
    }
    
    @GetMapping("/{id}/chunks")
    public ResponseEntity<List<DocumentChunk>> getChunks(@PathVariable String id) {
        LambdaQueryWrapper<DocumentChunk> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentChunk::getDocumentId, id)
                .orderByAsc(DocumentChunk::getChunkIndex);
        List<DocumentChunk> chunks = documentChunkMapper.selectList(wrapper);
        return ResponseEntity.ok(chunks);
    }
    
    @PostMapping("/{id}/reprocess")
    public ResponseEntity<DocumentDTO> reprocess(@PathVariable String id) {
        log.info("重新处理文档: {}", id);
        
        Document document = documentService.getById(id);
        if (document == null) {
            return ResponseEntity.notFound().build();
        }
        
        // 重置文档状态
        documentService.updateStatus(id, "PENDING", null);
        documentService.updateChunkCount(id, 0);
        
        // 删除旧的文档块
        LambdaQueryWrapper<DocumentChunk> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentChunk::getDocumentId, id);
        documentChunkMapper.delete(wrapper);
        
        // 重新触发异步处理
        documentVectorizationService.processDocument(id);
        
        // 返回更新后的文档信息
        document = documentService.getById(id);
        return ResponseEntity.ok(documentService.toDTO(document));
    }
}
