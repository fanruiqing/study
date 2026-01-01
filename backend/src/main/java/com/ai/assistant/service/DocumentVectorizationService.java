package com.ai.assistant.service;

import com.ai.assistant.entity.DocumentChunk;
import com.ai.assistant.entity.KnowledgeBase;
import com.ai.assistant.mapper.KnowledgeBaseMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentVectorizationService {
    
    private final DocumentProcessorService documentProcessorService;
    private final TextChunkingService textChunkingService;
    private final CustomVectorStoreService customVectorStoreService;
    private final DocumentService documentService;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final EmbeddingService embeddingService;
    
    /**
     * 异步处理文档
     */
    @Async
    public void processDocument(String documentId) {
        log.info("开始处理文档: {}", documentId);
        
        try {
            // 获取文档信息
            com.ai.assistant.entity.Document doc = documentService.getById(documentId);
            if (doc == null) {
                log.error("文档不存在: {}", documentId);
                return;
            }
            
            // 幂等性检查：如果文档已经在处理中或已完成，则跳过
            if ("PROCESSING".equals(doc.getStatus())) {
                log.warn("文档正在处理中，跳过重复处理: {}", documentId);
                return;
            }
            
            if ("COMPLETED".equals(doc.getStatus())) {
                log.warn("文档已处理完成，跳过重复处理: {}", documentId);
                return;
            }
            
            // 更新状态为处理中
            documentService.updateStatus(documentId, "PROCESSING", null);
            
            // 获取知识库配置
            KnowledgeBase kb = knowledgeBaseMapper.selectById(doc.getKnowledgeBaseId());
            if (kb == null) {
                throw new RuntimeException("知识库不存在: " + doc.getKnowledgeBaseId());
            }
            
            // 1. 提取文本
            String text = documentProcessorService.extractText(doc.getFilePath(), doc.getFileType());
            
            // 2. 分块
            List<TextChunkingService.TextChunk> textChunks = textChunkingService.splitIntoChunks(
                    text, kb.getChunkSize(), kb.getChunkOverlap());
            
            // 3. 保存文档块
            List<DocumentChunk> documentChunks = textChunkingService.saveChunks(documentId, textChunks);
            
            // 4. 向量化并存储
            // 批量生成嵌入向量，使用知识库指定的模型
            List<String> contents = documentChunks.stream()
                    .map(DocumentChunk::getContent)
                    .collect(Collectors.toList());
            
            log.info("使用模型 {} 批量生成嵌入向量，数量: {}", kb.getEmbeddingModel(), contents.size());
            List<float[]> embeddings = embeddingService.embedBatch(contents, kb.getEmbeddingModel());
            
            // 创建自定义向量数据列表
            List<CustomVectorStoreService.VectorData> vectorDataList = new ArrayList<>();
            for (int i = 0; i < documentChunks.size(); i++) {
                DocumentChunk chunk = documentChunks.get(i);
                float[] embedding = embeddings.get(i);
                
                // 创建元数据
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("documentId", documentId);
                metadata.put("documentName", doc.getFileName());
                metadata.put("knowledgeBaseId", doc.getKnowledgeBaseId());
                metadata.put("chunkIndex", chunk.getChunkIndex());
                metadata.put("chunkId", chunk.getId());
                
                // 创建向量数据（使用我们自己的存储，不依赖Spring AI）
                CustomVectorStoreService.VectorData vectorData = new CustomVectorStoreService.VectorData(
                    chunk.getId(),
                    chunk.getContent(),
                    embedding,
                    metadata
                );
                vectorDataList.add(vectorData);
                
                // 更新文档块的向量ID
                textChunkingService.updateVectorId(chunk.getId(), chunk.getId());
            }
            
            // 批量添加到自定义向量存储（完全控制，使用知识库指定的embedding）
            customVectorStoreService.addVectors(vectorDataList);
            
            // 更新文档状态和块数量
            documentService.updateChunkCount(documentId, documentChunks.size());
            documentService.updateStatus(documentId, "COMPLETED", null);
            
            log.info("文档处理完成: {}, 共 {} 个块，使用模型: {}", documentId, documentChunks.size(), kb.getEmbeddingModel());
            
        } catch (Exception e) {
            log.error("文档处理失败: {}", documentId, e);
            documentService.updateStatus(documentId, "FAILED", e.getMessage());
        }
    }
}
