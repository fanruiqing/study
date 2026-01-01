package com.ai.assistant.service;

import com.ai.assistant.entity.ModelProvider;
import com.ai.assistant.mapper.ModelProviderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {
    
    private final EmbeddingModel embeddingModel;
    private final ModelProviderMapper modelProviderMapper;
    
    /**
     * 生成单个文本的嵌入向量
     */
    public float[] embed(String text) {
        return embed(text, null);
    }
    
    /**
     * 使用指定模型生成单个文本的嵌入向量
     */
    public float[] embed(String text, String modelId) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("文本不能为空");
        }
        
        try {
            EmbeddingModel embeddingModel = getEmbeddingModel(modelId);
            EmbeddingResponse response = embeddingModel.embedForResponse(List.of(text));
            float[] embedding = response.getResults().get(0).getOutput();
            
            log.debug("生成嵌入向量，模型: {}, 文本长度: {}, 向量维度: {}", 
                     modelId != null ? modelId : "default", text.length(), embedding.length);
            return embedding;
        } catch (Exception e) {
            log.error("生成嵌入向量失败，模型: {}, 请检查API Key配置", modelId, e);
            throw new RuntimeException("生成嵌入向量失败，请在设置页面配置有效的OpenAI API Key", e);
        }
    }
    
    /**
     * 批量生成嵌入向量
     */
    public List<float[]> embedBatch(List<String> texts) {
        return embedBatch(texts, null);
    }
    
    /**
     * 使用指定模型批量生成嵌入向量
     */
    public List<float[]> embedBatch(List<String> texts, String modelId) {
        if (texts == null || texts.isEmpty()) {
            throw new IllegalArgumentException("文本列表不能为空");
        }
        
        try {
            log.info("========== 开始批量生成嵌入向量 ==========");
            log.info("请求模型: {}", modelId);
            log.info("文本数量: {}", texts.size());
            log.info("文本样例: {}", texts.get(0).substring(0, Math.min(100, texts.get(0).length())) + "...");
            
            EmbeddingModel embeddingModel = getEmbeddingModel(modelId);
            
            log.info("开始调用嵌入API...");
            long startTime = System.currentTimeMillis();
            
            EmbeddingResponse response = embeddingModel.embedForResponse(texts);
            
            long endTime = System.currentTimeMillis();
            log.info("API调用完成，耗时: {} ms", endTime - startTime);
            
            List<float[]> embeddings = response.getResults().stream()
                    .map(result -> result.getOutput())
                    .collect(Collectors.toList());
            
            log.info("批量生成嵌入向量完成，模型: {}, 数量: {}, 向量维度: {}", 
                    modelId != null ? modelId : "default", embeddings.size(), 
                    embeddings.isEmpty() ? 0 : embeddings.get(0).length);
            log.info("========== 嵌入向量生成完成 ==========");
            
            return embeddings;
        } catch (Exception e) {
            log.error("========== 嵌入向量生成失败 ==========");
            log.error("模型: {}", modelId);
            log.error("错误类型: {}", e.getClass().getName());
            log.error("错误信息: {}", e.getMessage());
            log.error("完整堆栈:", e);
            throw new RuntimeException("生成嵌入向量失败，请在设置页面配置有效的OpenAI API Key", e);
        }
    }
    
    /**
     * 获取当前使用的模型名称
     */
    public String getCurrentModel() {
        return "text-embedding-ada-002";
    }
    
    /**
     * 获取EmbeddingModel，优先使用数据库配置
     */
    private EmbeddingModel getEmbeddingModel() {
        return getEmbeddingModel(null);
    }
    
    /**
     * 获取指定模型的EmbeddingModel
     */
    private EmbeddingModel getEmbeddingModel(String modelId) {
        log.info("========== 开始查找嵌入模型提供商 ==========");
        log.info("目标模型: {}", modelId);
        
        // 尝试从数据库获取配置（支持OpenAI和兼容OpenAI API的提供商）
        try {
            List<ModelProvider> providers = modelProviderMapper.selectList(null);
            log.info("数据库中共有 {} 个提供商", providers.size());
            
            // 打印所有提供商信息
            for (ModelProvider p : providers) {
                log.info("提供商: {}, 类型: {}, 激活: {}, Base URL: {}", 
                        p.getName(), p.getType(), p.getIsActive(), p.getBaseUrl());
                if (p.getModelsJson() != null && !p.getModelsJson().isEmpty()) {
                    log.debug("  支持的模型: {}", p.getModelsJson().substring(0, Math.min(200, p.getModelsJson().length())));
                }
            }
            
            ModelProvider provider = null;
            
            // 如果指定了模型ID，尝试找到包含该模型的提供商
            if (modelId != null && !modelId.isEmpty()) {
                log.info("开始精确匹配，查找支持模型 {} 的提供商", modelId);
                
                // 遍历所有激活的提供商，查找包含该模型的提供商
                for (ModelProvider p : providers) {
                    if (!p.getIsActive()) {
                        log.debug("跳过未激活的提供商: {}", p.getName());
                        continue;
                    }
                    
                    if (!"OPENAI".equalsIgnoreCase(p.getType())) {
                        log.debug("跳过非OpenAI类型的提供商: {} (类型: {})", p.getName(), p.getType());
                        continue;
                    }
                    
                    // 检查提供商的模型列表中是否包含该模型
                    if (p.getModelsJson() != null && !p.getModelsJson().isEmpty()) {
                        if (p.getModelsJson().contains(modelId)) {
                            provider = p;
                            log.info("✓ 精确匹配成功! 找到支持模型 {} 的提供商: {}", modelId, p.getName());
                            break;
                        } else {
                            log.debug("提供商 {} 的模型列表不包含 {}", p.getName(), modelId);
                        }
                    }
                }
                
                // 如果没找到，尝试模糊匹配（通过模型名称前缀）
                if (provider == null) {
                    log.info("精确匹配失败，开始模糊匹配...");
                    
                    for (ModelProvider p : providers) {
                        if (!p.getIsActive() || !"OPENAI".equalsIgnoreCase(p.getType())) {
                            continue;
                        }
                        
                        // 检查模型名称是否以提供商名称开头，或者提供商支持该模型
                        String providerName = p.getName().toLowerCase();
                        String modelLower = modelId.toLowerCase();
                        
                        log.debug("尝试模糊匹配: 提供商={}, 模型={}", providerName, modelLower);
                        
                        // 特殊处理：硅基流动支持BAAI模型
                        if (providerName.contains("silicon") && modelLower.contains("baai")) {
                            provider = p;
                            log.info("✓ 模糊匹配成功! 提供商 {} (包含'silicon') 支持模型 {} (包含'baai')", 
                                    p.getName(), modelId);
                            break;
                        }
                        
                        // 特殊处理：NVIDIA支持BAAI模型
                        if (providerName.contains("nvidia") && modelLower.contains("baai")) {
                            provider = p;
                            log.info("✓ 模糊匹配成功! 提供商 {} (包含'nvidia') 支持模型 {} (包含'baai')", 
                                    p.getName(), modelId);
                            break;
                        }
                    }
                }
            }
            
            // 如果还没找到，使用第一个激活的OpenAI类型提供商
            if (provider == null) {
                log.warn("未找到匹配的提供商，使用默认的激活提供商");
                provider = providers.stream()
                        .filter(p -> "OPENAI".equalsIgnoreCase(p.getType()) && p.getIsActive())
                        .findFirst()
                        .orElse(null);
                
                if (provider != null) {
                    log.info("使用默认的激活提供商: {}", provider.getName());
                }
            }
            
            if (provider != null && provider.getApiKey() != null && !provider.getApiKey().isEmpty()) {
                log.info("========== 提供商选择完成 ==========");
                log.info("最终选择的提供商: {}", provider.getName());
                log.info("Base URL: {}", provider.getBaseUrl());
                log.info("模型: {}", modelId);
                log.info("API Key: {}...{}", 
                        provider.getApiKey().substring(0, Math.min(10, provider.getApiKey().length())),
                        provider.getApiKey().length() > 10 ? 
                            provider.getApiKey().substring(provider.getApiKey().length() - 4) : "");
                
                // 构造等效的curl命令用于调试
                String curlCommand = String.format(
                    "curl -X POST '%s/embeddings' \\\n" +
                    "  -H 'Authorization: Bearer %s' \\\n" +
                    "  -H 'Content-Type: application/json' \\\n" +
                    "  -d '{\"model\": \"%s\", \"input\": [\"测试文本\"]}'",
                    provider.getBaseUrl(),
                    provider.getApiKey().substring(0, Math.min(10, provider.getApiKey().length())) + "...",
                    modelId != null ? modelId : "text-embedding-ada-002"
                );
                log.info("等效的curl命令:\n{}", curlCommand);
                log.info("==========================================");
                
                OpenAiApi openAiApi = new OpenAiApi(provider.getBaseUrl(), provider.getApiKey());
                // 如果指定了模型ID，使用指定的模型，否则使用默认模型
                String actualModelId = modelId != null ? modelId : "text-embedding-ada-002";
                OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                        .model(actualModelId)
                        .build();
                return new OpenAiEmbeddingModel(openAiApi, MetadataMode.EMBED, options);
            } else {
                log.error("未找到可用的提供商或API Key为空");
            }
        } catch (Exception e) {
            log.error("从数据库获取API配置失败: {}", e.getMessage(), e);
        }
        
        // 使用默认的EmbeddingModel（从Spring配置）
        log.warn("使用默认的EmbeddingModel配置（从application.yml）");
        return embeddingModel;
    }
}
