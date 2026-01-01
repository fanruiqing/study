package com.ai.assistant.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VectorStoreService {
    
    private final VectorStore vectorStore;
    
    /**
     * 添加文档到向量存储
     */
    public void add(List<Document> documents) {
        vectorStore.add(documents);
        log.info("添加 {} 个文档到向量存储", documents.size());
    }
    
    /**
     * 语义相似度搜索
     */
    public List<Document> similaritySearch(String query, int topK) {
        SearchRequest request = SearchRequest.query(query).withTopK(topK);
        List<Document> results = vectorStore.similaritySearch(request);
        log.info("相似度搜索完成，查询: {}, 返回 {} 个结果", query, results.size());
        return results;
    }
    
    /**
     * 带阈值的相似度搜索
     */
    public List<Document> similaritySearchWithScore(String query, int topK, double threshold) {
        SearchRequest request = SearchRequest.query(query)
                .withTopK(topK)
                .withSimilarityThreshold(threshold);
        return vectorStore.similaritySearch(request);
    }
    
    /**
     * 按知识库ID过滤搜索
     */
    public List<Document> searchByKnowledgeBase(String query, List<String> knowledgeBaseIds, int topK) {
        // 先进行相似度搜索
        List<Document> allResults = similaritySearch(query, topK * 2);
        
        // 过滤出指定知识库的结果
        List<Document> filtered = allResults.stream()
                .filter(doc -> {
                    String kbId = (String) doc.getMetadata().get("knowledgeBaseId");
                    return knowledgeBaseIds.contains(kbId);
                })
                .limit(topK)
                .collect(Collectors.toList());
        
        log.info("知识库过滤搜索完成，返回 {} 个结果", filtered.size());
        return filtered;
    }
    
    /**
     * 删除文档
     */
    public void delete(List<String> ids) {
        vectorStore.delete(ids);
        log.info("从向量存储删除 {} 个文档", ids.size());
    }
    
    /**
     * 按知识库ID删除所有文档
     */
    public void deleteByKnowledgeBase(String knowledgeBaseId) {
        // 注意：Simple Vector Store 不支持按元数据删除
        // 这里需要先查询所有文档，然后过滤删除
        log.warn("Simple Vector Store 不支持按元数据批量删除，需要手动实现");
    }
}
