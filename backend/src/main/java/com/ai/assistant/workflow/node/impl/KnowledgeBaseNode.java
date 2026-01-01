package com.ai.assistant.workflow.node.impl;

import com.ai.assistant.dto.RetrievedChunk;
import com.ai.assistant.entity.KnowledgeBase;
import com.ai.assistant.service.KnowledgeBaseService;
import com.ai.assistant.service.RAGService;
import com.ai.assistant.workflow.exception.NodeExecutionException;
import com.ai.assistant.workflow.exception.ValidationException;
import com.ai.assistant.workflow.execution.ExecutionContext;
import com.ai.assistant.workflow.execution.NodeInput;
import com.ai.assistant.workflow.execution.NodeOutput;
import com.ai.assistant.workflow.execution.NodeStatus;
import com.ai.assistant.workflow.node.AbstractWorkflowNode;
import com.ai.assistant.workflow.node.NodeMetadata;
import com.ai.assistant.workflow.node.NodeParameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeBaseNode extends AbstractWorkflowNode {
    
    private final RAGService ragService;
    private final KnowledgeBaseService knowledgeBaseService;
    
    @Override
    public NodeOutput execute(NodeInput input, ExecutionContext context) throws NodeExecutionException {
        try {
            // 获取查询内容
            String query = (String) input.getData().get("query");
            if (query == null || query.trim().isEmpty()) {
                query = (String) input.getData().get("input");
            }
            
            log.info("知识库节点执行，查询: {}", query);
            
            // 获取配置
            Map<String, Object> config = input.getConfig();
            List<String> knowledgeBaseIds = getKnowledgeBaseIds(config);
            int topK = config.get("topK") != null ? ((Number) config.get("topK")).intValue() : 5;
            double similarityThreshold = config.get("similarityThreshold") != null ? 
                    ((Number) config.get("similarityThreshold")).doubleValue() : 0.0; // 默认不过滤
            
            log.info("知识库配置 - IDs: {}, topK: {}, threshold: {}", knowledgeBaseIds, topK, similarityThreshold);
            
            // 如果没有指定知识库，获取所有知识库
            if (knowledgeBaseIds.isEmpty()) {
                List<KnowledgeBase> allKbs = knowledgeBaseService.list();
                knowledgeBaseIds = allKbs.stream()
                        .map(KnowledgeBase::getId)
                        .collect(Collectors.toList());
                log.info("未指定知识库，使用所有知识库: {}", knowledgeBaseIds);
            }
            
            if (knowledgeBaseIds.isEmpty()) {
                log.warn("没有可用的知识库");
                return buildEmptyOutput();
            }
            
            // 获取嵌入模型
            String embeddingModel = getEmbeddingModel(knowledgeBaseIds);
            
            // 执行检索
            List<RetrievedChunk> allChunks = ragService.retrieve(query, knowledgeBaseIds, topK, embeddingModel);
            int totalRetrieved = allChunks.size();
            
            // 打印检索结果的相似度
            log.info("RAG检索返回 {} 个结果", totalRetrieved);
            for (RetrievedChunk chunk : allChunks) {
                log.info("  - 文档: {}, 相似度: {}", chunk.getDocumentName(), chunk.getRelevanceScore());
            }
            
            // 记录被过滤的结果
            List<RetrievedChunk> filteredOut = new ArrayList<>();
            List<RetrievedChunk> chunks = allChunks;
            
            // 过滤低相关度结果（如果设置了阈值）
            if (similarityThreshold > 0) {
                filteredOut = allChunks.stream()
                        .filter(c -> c.getRelevanceScore() < similarityThreshold)
                        .collect(Collectors.toList());
                chunks = allChunks.stream()
                        .filter(c -> c.getRelevanceScore() >= similarityThreshold)
                        .collect(Collectors.toList());
                log.info("过滤后剩余 {} 个结果，过滤掉 {} 个", chunks.size(), filteredOut.size());
            }
            
            log.info("检索到 {} 个相关文档片段", chunks.size());
            
            // 构建输出
            Map<String, Object> outputData = new HashMap<>();
            
            // 结果列表
            List<Map<String, Object>> results = chunks.stream()
                    .map(chunk -> {
                        Map<String, Object> result = new HashMap<>();
                        result.put("content", chunk.getContent());
                        result.put("documentName", chunk.getDocumentName());
                        result.put("documentId", chunk.getDocumentId());
                        result.put("score", chunk.getRelevanceScore());
                        return result;
                    })
                    .collect(Collectors.toList());
            
            // 被过滤掉的结果（用于调试）
            List<Map<String, Object>> filteredResults = filteredOut.stream()
                    .map(chunk -> {
                        Map<String, Object> result = new HashMap<>();
                        result.put("content", chunk.getContent().length() > 100 ? 
                                chunk.getContent().substring(0, 100) + "..." : chunk.getContent());
                        result.put("documentName", chunk.getDocumentName());
                        result.put("score", chunk.getRelevanceScore());
                        result.put("reason", String.format("相似度 %.2f 低于阈值 %.2f", 
                                chunk.getRelevanceScore(), similarityThreshold));
                        return result;
                    })
                    .collect(Collectors.toList());
            
            // 合并上下文
            String combinedContext = chunks.stream()
                    .map(RetrievedChunk::getContent)
                    .collect(Collectors.joining("\n\n---\n\n"));
            
            outputData.put("results", results);
            outputData.put("context", combinedContext);
            outputData.put("query", query);
            
            // 添加检索统计信息
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalRetrieved", totalRetrieved);
            stats.put("afterFilter", chunks.size());
            stats.put("filteredOut", filteredOut.size());
            stats.put("threshold", similarityThreshold);
            stats.put("topK", topK);
            outputData.put("_stats", stats);
            
            // 如果有被过滤的结果，添加到输出中供调试
            if (!filteredResults.isEmpty()) {
                outputData.put("_filteredOut", filteredResults);
            }
            
            outputData.put("results", results);
            outputData.put("context", combinedContext);
            outputData.put("query", query);
            
            return NodeOutput.builder()
                    .data(outputData)
                    .status(NodeStatus.COMPLETED)
                    .build();
                    
        } catch (Exception e) {
            log.error("知识库节点执行失败", e);
            throw new NodeExecutionException("Knowledge base node execution failed: " + e.getMessage(), e);
        }
    }
    
    private List<String> getKnowledgeBaseIds(Map<String, Object> config) {
        Object kbIds = config.get("knowledgeBaseIds");
        if (kbIds == null) {
            kbIds = config.get("knowledgeBaseId");
        }
        
        if (kbIds instanceof List) {
            return ((List<?>) kbIds).stream()
                    .map(Object::toString)
                    .collect(Collectors.toList());
        } else if (kbIds instanceof String) {
            String id = (String) kbIds;
            if (!id.isEmpty()) {
                return List.of(id);
            }
        }
        return new ArrayList<>();
    }
    
    private String getEmbeddingModel(List<String> knowledgeBaseIds) {
        // 尝试从第一个知识库获取嵌入模型配置
        if (!knowledgeBaseIds.isEmpty()) {
            try {
                KnowledgeBase kb = knowledgeBaseService.getById(knowledgeBaseIds.get(0));
                if (kb != null && kb.getEmbeddingModel() != null) {
                    return kb.getEmbeddingModel();
                }
            } catch (Exception e) {
                log.warn("获取知识库嵌入模型失败", e);
            }
        }
        return "text-embedding-3-small"; // 默认模型
    }
    
    private NodeOutput buildEmptyOutput() {
        Map<String, Object> outputData = new HashMap<>();
        outputData.put("results", List.of());
        outputData.put("context", "");
        return NodeOutput.builder()
                .data(outputData)
                .status(NodeStatus.COMPLETED)
                .build();
    }
    
    @Override
    public void validateConfig(Map<String, Object> config) throws ValidationException {
        // 知识库ID可以为空（使用所有知识库）
    }
    
    @Override
    public String getType() {
        return "knowledge_base";
    }
    
    @Override
    public NodeMetadata getMetadata() {
        return NodeMetadata.builder()
                .type("knowledge_base")
                .name("知识库节点")
                .description("检索知识库")
                .icon("database")
                .category("AI")
                .parameters(Arrays.asList(
                        NodeParameter.builder()
                                .name("knowledgeBaseIds")
                                .label("知识库ID")
                                .type("array")
                                .required(false)
                                .build(),
                        NodeParameter.builder()
                                .name("topK")
                                .label("返回数量")
                                .type("number")
                                .defaultValue(3)
                                .required(false)
                                .build(),
                        NodeParameter.builder()
                                .name("similarityThreshold")
                                .label("相似度阈值")
                                .type("number")
                                .defaultValue(0.5)
                                .required(false)
                                .build()
                ))
                .inputPorts(Arrays.asList("input"))
                .outputPorts(Arrays.asList("output"))
                .build();
    }
}
