package com.ai.assistant.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 自定义向量存储服务
 * 不依赖Spring AI的VectorStore,直接管理向量数据
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomVectorStoreService {
    
    private final ObjectMapper objectMapper;
    private static final String VECTOR_STORE_PATH = "uploads/custom-vector-store";
    
    /**
     * 向量数据结构
     */
    public static class VectorData {
        private String id;
        private String content;
        private float[] embedding;
        private Map<String, Object> metadata;
        
        public VectorData() {}
        
        public VectorData(String id, String content, float[] embedding, Map<String, Object> metadata) {
            this.id = id;
            this.content = content;
            this.embedding = embedding;
            this.metadata = metadata;
        }
        
        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public float[] getEmbedding() { return embedding; }
        public void setEmbedding(float[] embedding) { this.embedding = embedding; }
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    }
    
    /**
     * 搜索结果
     */
    public static class SearchResult {
        private String id;
        private String content;
        private Map<String, Object> metadata;
        private double similarity;
        
        public SearchResult(String id, String content, Map<String, Object> metadata, double similarity) {
            this.id = id;
            this.content = content;
            this.metadata = metadata;
            this.similarity = similarity;
        }
        
        // Getters
        public String getId() { return id; }
        public String getContent() { return content; }
        public Map<String, Object> getMetadata() { return metadata; }
        public double getSimilarity() { return similarity; }
    }
    
    /**
     * 添加向量数据
     */
    public void addVectors(List<VectorData> vectors) {
        try {
            // 确保目录存在
            Path storePath = Paths.get(VECTOR_STORE_PATH);
            Files.createDirectories(storePath);
            
            // 按知识库ID分组存储
            Map<String, List<VectorData>> groupedByKb = vectors.stream()
                    .collect(Collectors.groupingBy(v -> 
                        (String) v.getMetadata().get("knowledgeBaseId")));
            
            for (Map.Entry<String, List<VectorData>> entry : groupedByKb.entrySet()) {
                String kbId = entry.getKey();
                List<VectorData> kbVectors = entry.getValue();
                
                // 读取现有数据
                List<VectorData> existingVectors = loadVectorsByKnowledgeBase(kbId);
                
                // 合并新数据
                existingVectors.addAll(kbVectors);
                
                // 保存到文件
                saveVectorsByKnowledgeBase(kbId, existingVectors);
            }
            
            log.info("成功添加 {} 个向量到自定义存储", vectors.size());
            
        } catch (Exception e) {
            log.error("添加向量失败", e);
            throw new RuntimeException("添加向量失败: " + e.getMessage());
        }
    }
    
    /**
     * 相似度搜索
     */
    public List<SearchResult> similaritySearch(String query, float[] queryEmbedding, 
                                               List<String> knowledgeBaseIds, int topK) {
        try {
            List<SearchResult> allResults = new ArrayList<>();
            
            // 从指定的知识库中搜索
            for (String kbId : knowledgeBaseIds) {
                List<VectorData> vectors = loadVectorsByKnowledgeBase(kbId);
                
                for (VectorData vector : vectors) {
                    double similarity = cosineSimilarity(queryEmbedding, vector.getEmbedding());
                    allResults.add(new SearchResult(
                        vector.getId(),
                        vector.getContent(),
                        vector.getMetadata(),
                        similarity
                    ));
                }
            }
            
            // 按相似度排序并返回topK
            return allResults.stream()
                    .sorted((a, b) -> Double.compare(b.getSimilarity(), a.getSimilarity()))
                    .limit(topK)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("相似度搜索失败", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 删除知识库的所有向量
     */
    public void deleteByKnowledgeBase(String knowledgeBaseId) {
        try {
            Path filePath = Paths.get(VECTOR_STORE_PATH, knowledgeBaseId + ".json");
            Files.deleteIfExists(filePath);
            log.info("删除知识库 {} 的向量数据", knowledgeBaseId);
        } catch (Exception e) {
            log.error("删除向量数据失败", e);
        }
    }
    
    /**
     * 删除指定的向量
     */
    public void deleteVectors(List<String> ids, String knowledgeBaseId) {
        try {
            List<VectorData> vectors = loadVectorsByKnowledgeBase(knowledgeBaseId);
            vectors.removeIf(v -> ids.contains(v.getId()));
            saveVectorsByKnowledgeBase(knowledgeBaseId, vectors);
            log.info("删除 {} 个向量", ids.size());
        } catch (Exception e) {
            log.error("删除向量失败", e);
        }
    }
    
    /**
     * 计算余弦相似度
     */
    private double cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("向量维度不匹配");
        }
        
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
    
    /**
     * 加载知识库的向量数据
     */
    private List<VectorData> loadVectorsByKnowledgeBase(String knowledgeBaseId) {
        try {
            Path filePath = Paths.get(VECTOR_STORE_PATH, knowledgeBaseId + ".json");
            if (!Files.exists(filePath)) {
                return new ArrayList<>();
            }
            
            String json = Files.readString(filePath);
            VectorData[] vectors = objectMapper.readValue(json, VectorData[].class);
            return new ArrayList<>(Arrays.asList(vectors));
            
        } catch (Exception e) {
            log.error("加载向量数据失败: {}", knowledgeBaseId, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 保存知识库的向量数据
     */
    private void saveVectorsByKnowledgeBase(String knowledgeBaseId, List<VectorData> vectors) {
        try {
            Path filePath = Paths.get(VECTOR_STORE_PATH, knowledgeBaseId + ".json");
            String json = objectMapper.writeValueAsString(vectors);
            Files.writeString(filePath, json);
        } catch (Exception e) {
            log.error("保存向量数据失败: {}", knowledgeBaseId, e);
            throw new RuntimeException("保存向量数据失败: " + e.getMessage());
        }
    }
}
