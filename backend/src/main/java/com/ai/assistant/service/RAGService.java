package com.ai.assistant.service;

import com.ai.assistant.dto.RetrievedChunk;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RAGService {
    
    private final CustomVectorStoreService customVectorStoreService;
    private final EmbeddingService embeddingService;
    
    /**
     * 检索相关文档
     */
    public List<RetrievedChunk> retrieve(String query, List<String> knowledgeBaseIds, int topK, String embeddingModel) {
        log.info("开始检索，查询: {}, 知识库数量: {}, topK: {}, 模型: {}", 
                query, knowledgeBaseIds.size(), topK, embeddingModel);
        
        try {
            // 生成查询的embedding向量
            float[] queryEmbedding = embeddingService.embed(query, embeddingModel);
            
            // 使用自定义向量存储进行相似度搜索
            List<CustomVectorStoreService.SearchResult> results = 
                customVectorStoreService.similaritySearch(query, queryEmbedding, knowledgeBaseIds, topK);
            
            // 转换为 RetrievedChunk
            List<RetrievedChunk> chunks = results.stream()
                    .map(result -> RetrievedChunk.builder()
                            .chunkId((String) result.getMetadata().get("chunkId"))
                            .documentId((String) result.getMetadata().get("documentId"))
                            .documentName((String) result.getMetadata().get("documentName"))
                            .content(result.getContent())
                            .relevanceScore((float) result.getSimilarity())
                            .metadata(result.getMetadata())
                            .build())
                    .collect(Collectors.toList());
            
            log.info("检索完成，返回 {} 个结果", chunks.size());
            return chunks;
            
        } catch (Exception e) {
            log.error("检索失败", e);
            return List.of();
        }
    }
    
    /**
     * 构建带上下文的提示词
     */
    public String buildPromptWithContext(String userMessage, List<RetrievedChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return userMessage;
        }
        
        StringBuilder context = new StringBuilder();
        context.append("以下是相关的知识库内容：\n\n");
        
        for (int i = 0; i < chunks.size(); i++) {
            RetrievedChunk chunk = chunks.get(i);
            context.append(String.format("[文档 %d: %s]\n", i + 1, chunk.getDocumentName()));
            context.append(chunk.getContent());
            context.append("\n\n");
        }
        
        context.append("请基于以上知识库内容回答用户的问题。如果知识库中没有相关信息，请说明。\n\n");
        context.append("用户问题：").append(userMessage);
        
        return context.toString();
    }
}
