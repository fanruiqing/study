package com.ai.assistant.service;

import com.ai.assistant.dto.KnowledgeBaseDTO;
import com.ai.assistant.entity.KnowledgeBase;
import com.ai.assistant.entity.ModelProvider;
import com.ai.assistant.entity.SessionKnowledgeBase;
import com.ai.assistant.mapper.KnowledgeBaseMapper;
import com.ai.assistant.mapper.ModelProviderMapper;
import com.ai.assistant.mapper.SessionKnowledgeBaseMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseService {
    
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final SessionKnowledgeBaseMapper sessionKnowledgeBaseMapper;
    private final ModelProviderMapper modelProviderMapper;
    
    /**
     * 创建知识库
     */
    @Transactional
    public KnowledgeBase create(KnowledgeBaseDTO dto) {
        long now = System.currentTimeMillis();
        
        // 查找匹配的提供商
        ModelProvider provider = findProviderForModel(dto.getEmbeddingModel());
        
        KnowledgeBase knowledgeBase = KnowledgeBase.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .embeddingModel(dto.getEmbeddingModel())
                .providerId(provider != null ? provider.getId() : null)
                .providerName(provider != null ? provider.getName() : null)
                .chunkSize(dto.getChunkSize())
                .chunkOverlap(dto.getChunkOverlap())
                .createdAt(now)
                .updatedAt(now)
                .build();
        
        knowledgeBaseMapper.insert(knowledgeBase);
        log.info("创建知识库成功: {}, ID: {}, 提供商: {}", 
                knowledgeBase.getName(), knowledgeBase.getId(), 
                provider != null ? provider.getName() : "未知");
        
        return knowledgeBase;
    }
    
    /**
     * 更新知识库
     */
    @Transactional
    public KnowledgeBase update(String id, KnowledgeBaseDTO dto) {
        KnowledgeBase existing = knowledgeBaseMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("知识库不存在: " + id);
        }
        
        // 查找匹配的提供商
        ModelProvider provider = findProviderForModel(dto.getEmbeddingModel());
        
        KnowledgeBase knowledgeBase = KnowledgeBase.builder()
                .id(id)
                .name(dto.getName())
                .description(dto.getDescription())
                .embeddingModel(dto.getEmbeddingModel())
                .providerId(provider != null ? provider.getId() : null)
                .providerName(provider != null ? provider.getName() : null)
                .chunkSize(dto.getChunkSize())
                .chunkOverlap(dto.getChunkOverlap())
                .updatedAt(System.currentTimeMillis())
                .build();
        
        knowledgeBaseMapper.updateById(knowledgeBase);
        log.info("更新知识库成功: {}, 提供商: {}", id, 
                provider != null ? provider.getName() : "未知");
        
        return knowledgeBaseMapper.selectById(id);
    }
    
    /**
     * 根据模型名称查找匹配的提供商
     */
    private ModelProvider findProviderForModel(String modelId) {
        if (modelId == null || modelId.isEmpty()) {
            return null;
        }
        
        try {
            List<ModelProvider> providers = modelProviderMapper.selectList(null);
            
            // 精确匹配：查找模型列表中包含该模型的提供商
            for (ModelProvider p : providers) {
                if (!p.getIsActive() || !"OPENAI".equalsIgnoreCase(p.getType())) {
                    continue;
                }
                
                if (p.getModelsJson() != null && p.getModelsJson().contains(modelId)) {
                    log.info("为模型 {} 找到提供商: {}", modelId, p.getName());
                    return p;
                }
            }
            
            // 模糊匹配
            String modelLower = modelId.toLowerCase();
            for (ModelProvider p : providers) {
                if (!p.getIsActive() || !"OPENAI".equalsIgnoreCase(p.getType())) {
                    continue;
                }
                
                String providerName = p.getName().toLowerCase();
                
                // 硅基流动支持BAAI模型
                if ((providerName.contains("silicon") || providerName.contains("硅基")) 
                        && modelLower.contains("baai")) {
                    log.info("为模型 {} 找到提供商: {} (模糊匹配)", modelId, p.getName());
                    return p;
                }
                
                // NVIDIA支持BAAI模型
                if ((providerName.contains("nvidia") || providerName.contains("英伟达")) 
                        && modelLower.contains("baai")) {
                    log.info("为模型 {} 找到提供商: {} (模糊匹配)", modelId, p.getName());
                    return p;
                }
            }
            
            log.warn("未找到模型 {} 的匹配提供商", modelId);
        } catch (Exception e) {
            log.error("查找提供商失败: {}", e.getMessage(), e);
        }
        
        return null;
    }
    
    /**
     * 删除知识库
     */
    @Transactional
    public void delete(String id) {
        KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectById(id);
        if (knowledgeBase == null) {
            throw new IllegalArgumentException("知识库不存在: " + id);
        }
        
        // 删除知识库（级联删除文档和关联）
        knowledgeBaseMapper.deleteById(id);
        log.info("删除知识库成功: {}", id);
    }
    
    /**
     * 获取知识库详情
     */
    public KnowledgeBase getById(String id) {
        return knowledgeBaseMapper.selectById(id);
    }
    
    /**
     * 获取所有知识库
     */
    public List<KnowledgeBase> list() {
        LambdaQueryWrapper<KnowledgeBase> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(KnowledgeBase::getCreatedAt);
        return knowledgeBaseMapper.selectList(wrapper);
    }
    
    /**
     * 关联会话和知识库
     */
    @Transactional
    public void associateWithSession(String sessionId, List<String> knowledgeBaseIds) {
        // 先删除现有关联
        LambdaQueryWrapper<SessionKnowledgeBase> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(SessionKnowledgeBase::getSessionId, sessionId);
        sessionKnowledgeBaseMapper.delete(deleteWrapper);
        
        // 创建新关联
        long now = System.currentTimeMillis();
        for (String kbId : knowledgeBaseIds) {
            SessionKnowledgeBase association = SessionKnowledgeBase.builder()
                    .sessionId(sessionId)
                    .knowledgeBaseId(kbId)
                    .createdAt(now)
                    .build();
            sessionKnowledgeBaseMapper.insert(association);
        }
        
        log.info("会话 {} 关联知识库成功，数量: {}", sessionId, knowledgeBaseIds.size());
    }
    
    /**
     * 获取会话关联的知识库
     */
    public List<KnowledgeBase> getBySessionId(String sessionId) {
        LambdaQueryWrapper<SessionKnowledgeBase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SessionKnowledgeBase::getSessionId, sessionId);
        List<SessionKnowledgeBase> associations = sessionKnowledgeBaseMapper.selectList(wrapper);
        
        if (associations.isEmpty()) {
            return List.of();
        }
        
        List<String> kbIds = associations.stream()
                .map(SessionKnowledgeBase::getKnowledgeBaseId)
                .collect(Collectors.toList());
        
        return knowledgeBaseMapper.selectBatchIds(kbIds);
    }
    
    /**
     * 更新所有知识库的提供商信息
     */
    @Transactional
    public int updateAllProvidersInfo() {
        List<KnowledgeBase> allKbs = list();
        int updated = 0;
        
        for (KnowledgeBase kb : allKbs) {
            // 如果已经有提供商信息，跳过
            if (kb.getProviderId() != null && kb.getProviderName() != null) {
                continue;
            }
            
            // 查找匹配的提供商
            ModelProvider provider = findProviderForModel(kb.getEmbeddingModel());
            if (provider != null) {
                kb.setProviderId(provider.getId());
                kb.setProviderName(provider.getName());
                kb.setUpdatedAt(System.currentTimeMillis());
                knowledgeBaseMapper.updateById(kb);
                updated++;
                log.info("更新知识库 {} 的提供商信息: {}", kb.getName(), provider.getName());
            }
        }
        
        log.info("批量更新提供商信息完成，共更新 {} 个知识库", updated);
        return updated;
    }
    
    /**
     * 获取模型提供商
     */
    public ModelProvider getModelProvider(String providerId) {
        return modelProviderMapper.selectById(providerId);
    }
}
