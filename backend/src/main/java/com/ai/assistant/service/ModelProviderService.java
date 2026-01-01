package com.ai.assistant.service;

import com.ai.assistant.dto.ModelInfo;
import com.ai.assistant.dto.ModelProviderDTO;
import com.ai.assistant.entity.ModelProvider;
import com.ai.assistant.mapper.ModelProviderMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModelProviderService {
    
    private final ModelProviderMapper modelProviderMapper;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();
    
    public List<ModelProviderDTO> getAllProviders() {
        return modelProviderMapper.selectList(null)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    public List<ModelProviderDTO> getActiveProviders() {
        return modelProviderMapper.findByIsActiveTrue()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    public ModelProviderDTO getProvider(String id) {
        ModelProvider provider = modelProviderMapper.selectById(id);
        if (provider == null) {
            throw new RuntimeException("Provider not found: " + id);
        }
        return toDTO(provider);
    }
    
    public ModelProviderDTO createProvider(ModelProviderDTO dto) {
        ModelProvider provider = dto.toEntity();
        if (provider.getId() == null) {
            provider.setId(UUID.randomUUID().toString());
        }
        
        // 处理模型列表
        try {
            if (dto.getModelInfos() != null && !dto.getModelInfos().isEmpty()) {
                // 如果提供了详细的模型信息，直接使用
                provider.setModelsJson(objectMapper.writeValueAsString(dto.getModelInfos()));
            } else if (dto.getModels() != null && !dto.getModels().isEmpty()) {
                // 如果只提供了模型ID列表，转换为ModelInfo列表
                List<ModelInfo> modelInfos = dto.getModels().stream()
                        .map(ModelInfo::fromId)
                        .collect(Collectors.toList());
                provider.setModelsJson(objectMapper.writeValueAsString(modelInfos));
            } else if (dto.getApiKey() != null && dto.getBaseUrl() != null) {
                // 如果没有提供模型列表，自动获取
                List<ModelInfo> models = fetchAvailableModelsWithInfo(dto.getBaseUrl(), dto.getApiKey());
                provider.setModelsJson(objectMapper.writeValueAsString(models));
            }
        } catch (Exception e) {
            log.warn("Failed to set models for provider: {}", dto.getName(), e);
        }
        
        modelProviderMapper.insert(provider);
        return toDTO(provider);
    }
    
    public ModelProviderDTO updateProvider(String id, ModelProviderDTO dto) {
        ModelProvider provider = modelProviderMapper.selectById(id);
        if (provider == null) {
            throw new RuntimeException("Provider not found: " + id);
        }
        
        log.info("Updating provider: {}", id);
        log.info("DTO modelInfos count: {}", dto.getModelInfos() != null ? dto.getModelInfos().size() : "null");
        log.info("DTO models count: {}", dto.getModels() != null ? dto.getModels().size() : "null");
        
        if (dto.getName() != null) {
            provider.setName(dto.getName());
        }
        if (dto.getApiKey() != null && !dto.getApiKey().contains("****")) {
            provider.setApiKey(dto.getApiKey());
        }
        if (dto.getBaseUrl() != null) {
            provider.setBaseUrl(dto.getBaseUrl());
        }
        if (dto.getModelName() != null) {
            provider.setModelName(dto.getModelName());
        }
        if (dto.getIsActive() != null) {
            provider.setIsActive(dto.getIsActive());
        }
        if (dto.getTemperature() != null) {
            provider.setTemperature(dto.getTemperature());
        }
        if (dto.getMaxTokens() != null) {
            provider.setMaxTokens(dto.getMaxTokens());
        }
        
        // 处理模型列表更新
        try {
            if (dto.getModelInfos() != null && !dto.getModelInfos().isEmpty()) {
                // 如果提供了详细的模型信息，直接使用
                log.info("Using modelInfos from DTO, count: {}", dto.getModelInfos().size());
                provider.setModelsJson(objectMapper.writeValueAsString(dto.getModelInfos()));
            } else if (dto.getModels() != null && !dto.getModels().isEmpty()) {
                // 如果只提供了模型ID列表，转换为ModelInfo列表
                log.info("Converting models to ModelInfo, count: {}", dto.getModels().size());
                List<ModelInfo> modelInfos = dto.getModels().stream()
                        .map(ModelInfo::fromId)
                        .collect(Collectors.toList());
                provider.setModelsJson(objectMapper.writeValueAsString(modelInfos));
            } else {
                log.info("No model updates provided");
            }
        } catch (Exception e) {
            log.warn("Failed to update models for provider: {}", id, e);
        }
        
        modelProviderMapper.updateById(provider);
        return toDTO(provider);
    }
    
    public void deleteProvider(String id) {
        modelProviderMapper.deleteById(id);
    }
    
    public Map<String, Object> testConnection(String id) {
        ModelProvider provider = modelProviderMapper.selectById(id);
        if (provider == null) {
            throw new RuntimeException("Provider not found: " + id);
        }
        
        return testConnectionWithUrl(provider.getBaseUrl(), provider.getApiKey());
    }
    
    /**
     * 测试单个模型
     */
    public Map<String, Object> testSingleModel(String providerId, String modelId) {
        ModelProvider provider = modelProviderMapper.selectById(providerId);
        if (provider == null) {
            throw new RuntimeException("Provider not found: " + providerId);
        }
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("测试模型: {} - {}", provider.getName(), modelId);
            
            // 判断是否为embedding模型
            boolean isEmbeddingModel = isEmbeddingModel(modelId);
            
            if (isEmbeddingModel) {
                // 测试embedding模型
                result = testEmbeddingModel(provider, modelId);
            } else {
                // 测试聊天模型
                result = testChatModel(provider, modelId);
            }
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "模型测试失败: " + e.getMessage());
            log.error("模型测试失败: {} - {}", providerId, modelId, e);
            
            // 标记模型为失败
            markModelAsFailed(providerId, modelId);
        }
        
        return result;
    }
    
    /**
     * 测试聊天模型
     */
    private Map<String, Object> testChatModel(ModelProvider provider, String modelId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 创建 OpenAI API 客户端
            OpenAiApi openAiApi = new OpenAiApi(provider.getBaseUrl(), provider.getApiKey());
            
            // 创建聊天选项
            OpenAiChatOptions options = OpenAiChatOptions.builder()
                    .model(modelId)
                    .temperature(0.7)
                    .maxTokens(100)
                    .build();
            
            // 创建聊天模型
            OpenAiChatModel chatModel = new OpenAiChatModel(openAiApi, options);
            
            // 发送测试消息
            long startTime = System.currentTimeMillis();
            String response = chatModel.call("Hello, this is a test message. Please respond with 'OK'.");
            long latency = System.currentTimeMillis() - startTime;
            
            result.put("success", true);
            result.put("message", "聊天模型测试成功");
            result.put("latency", latency);
            result.put("response", response);
            
            // 测试成功，清除失败标记
            clearModelFailedMark(provider.getId(), modelId);
            
            log.info("聊天模型测试成功: {} - {} ({}ms)", provider.getName(), modelId, latency);
            
        } catch (Exception e) {
            throw new RuntimeException("聊天模型测试失败: " + e.getMessage(), e);
        }
        
        return result;
    }
    
    /**
     * 测试embedding模型
     */
    private Map<String, Object> testEmbeddingModel(ModelProvider provider, String modelId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String url = provider.getBaseUrl().endsWith("/") 
                ? provider.getBaseUrl() + "v1/embeddings" 
                : provider.getBaseUrl() + "/v1/embeddings";
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + provider.getApiKey());
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", modelId);
            requestBody.put("input", "This is a test message for embedding model.");
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            long startTime = System.currentTimeMillis();
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class);
            long latency = System.currentTimeMillis() - startTime;
            
            if (response.getStatusCode() == HttpStatus.OK) {
                result.put("success", true);
                result.put("message", "Embedding模型测试成功");
                result.put("latency", latency);
                
                // 解析响应获取向量维度
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode data = root.get("data");
                if (data != null && data.isArray() && data.size() > 0) {
                    JsonNode embedding = data.get(0).get("embedding");
                    if (embedding != null && embedding.isArray()) {
                        result.put("dimensions", embedding.size());
                    }
                }
                
                // 测试成功，清除失败标记
                clearModelFailedMark(provider.getId(), modelId);
                
                log.info("Embedding模型测试成功: {} - {} ({}ms)", provider.getName(), modelId, latency);
            } else {
                throw new RuntimeException("HTTP " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            throw new RuntimeException("Embedding模型测试失败: " + e.getMessage(), e);
        }
        
        return result;
    }
    
    /**
     * 判断是否为embedding模型
     */
    private boolean isEmbeddingModel(String modelId) {
        if (modelId == null) {
            return false;
        }
        
        String id = modelId.toLowerCase();
        
        // 常见的embedding模型关键词
        String[] keywords = {
            "embedding", "embed", "bge", "gte", "m3e", 
            "text2vec", "sentence", "e5", "instructor", 
            "multilingual-e5", "paraphrase", "distiluse",
            "text-embedding"
        };
        
        for (String keyword : keywords) {
            if (id.contains(keyword)) {
                return true;
            }
        }
        
        return false;
    }
    
    public Map<String, Object> testConnectionWithUrl(String baseUrl, String apiKey) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 先尝试获取模型列表来验证连接
            String url = baseUrl.endsWith("/") ? baseUrl + "v1/models" : baseUrl + "/v1/models";
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + apiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            long startTime = System.currentTimeMillis();
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);
            long latency = System.currentTimeMillis() - startTime;
            
            if (response.getStatusCode() == HttpStatus.OK) {
                result.put("success", true);
                result.put("message", "连接成功");
                result.put("latency", latency);
                
                // 解析模型数量
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode data = root.get("data");
                if (data != null && data.isArray()) {
                    result.put("modelCount", data.size());
                }
            } else {
                result.put("success", false);
                result.put("message", "连接失败: HTTP " + response.getStatusCode());
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "连接失败: " + e.getMessage());
            log.error("Connection test failed", e);
        }
        
        return result;
    }
    
    /**
     * 获取可用模型列表（带详细信息）
     */
    public List<ModelInfo> fetchModelsWithInfo(String baseUrl, String apiKey) {
        return fetchAvailableModelsWithInfo(baseUrl, apiKey);
    }
    
    /**
     * 获取分组后的模型列表
     */
    public Map<String, List<ModelInfo>> fetchModelsGrouped(String baseUrl, String apiKey) {
        List<ModelInfo> models = fetchAvailableModelsWithInfo(baseUrl, apiKey);
        return models.stream()
                .collect(Collectors.groupingBy(
                        m -> m.getGroup() != null ? m.getGroup() : "other",
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }
    
    /**
     * 刷新提供商的模型列表
     */
    public ModelProviderDTO refreshModels(String id) {
        ModelProvider provider = modelProviderMapper.selectById(id);
        if (provider == null) {
            throw new RuntimeException("Provider not found: " + id);
        }
        
        try {
            String baseUrl = provider.getBaseUrl() != null ? provider.getBaseUrl() : "https://api.openai.com";
            List<ModelInfo> models = fetchAvailableModelsWithInfo(baseUrl, provider.getApiKey());
            provider.setModelsJson(objectMapper.writeValueAsString(models));
            modelProviderMapper.updateById(provider);
        } catch (Exception e) {
            log.error("Failed to refresh models for provider: {}", id, e);
            throw new RuntimeException("获取模型列表失败: " + e.getMessage());
        }
        
        return toDTO(provider);
    }
    
    /**
     * 调用 OpenAI 兼容的 /v1/models 接口获取模型列表
     */
    private List<ModelInfo> fetchAvailableModelsWithInfo(String baseUrl, String apiKey) {
        List<ModelInfo> models = new ArrayList<>();
        
        try {
            String url = baseUrl.endsWith("/") ? baseUrl + "v1/models" : baseUrl + "/v1/models";
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + apiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode data = root.get("data");
                
                if (data != null && data.isArray()) {
                    for (JsonNode model : data) {
                        String modelId = model.get("id").asText();
                        ModelInfo info = ModelInfo.fromId(modelId);
                        
                        // 尝试从 API 响应中获取更多信息
                        if (model.has("owned_by")) {
                            String ownedBy = model.get("owned_by").asText();
                            if (info.getGroup().equals("other")) {
                                info.setGroup(ownedBy);
                            }
                        }
                        
                        models.add(info);
                    }
                }
            }
            
            // 按分组和名称排序
            models.sort((a, b) -> {
                int groupCompare = getGroupOrder(a.getGroup()) - getGroupOrder(b.getGroup());
                if (groupCompare != 0) return groupCompare;
                return a.getId().compareTo(b.getId());
            });
            
        } catch (Exception e) {
            log.error("Failed to fetch models from {}: {}", baseUrl, e.getMessage());
            throw new RuntimeException("获取模型列表失败: " + e.getMessage());
        }
        
        return models;
    }
    
    private int getGroupOrder(String group) {
        return switch (group) {
            case "gpt-4" -> 1;
            case "gpt-3.5" -> 2;
            case "claude" -> 3;
            case "deepseek" -> 4;
            case "qwen" -> 5;
            case "llama" -> 6;
            case "mistral" -> 7;
            case "gemini" -> 8;
            case "nvidia" -> 9;
            case "embedding" -> 90;
            case "image" -> 91;
            case "speech" -> 92;
            case "whisper" -> 93;
            default -> 50;
        };
    }
    
    private ModelProviderDTO toDTO(ModelProvider provider) {
        ModelProviderDTO dto = ModelProviderDTO.fromEntity(provider);
        
        // 解析失败模型列表
        List<String> failedModelIds = new ArrayList<>();
        if (provider.getFailedModels() != null) {
            try {
                failedModelIds = objectMapper.readValue(
                        provider.getFailedModels(),
                        new TypeReference<List<String>>() {}
                );
            } catch (Exception e) {
                log.warn("Failed to parse failed models JSON", e);
            }
        }
        
        // 解析模型列表
        if (provider.getModelsJson() != null) {
            try {
                // 尝试解析为 ModelInfo 列表
                List<ModelInfo> modelInfos = objectMapper.readValue(
                        provider.getModelsJson(), 
                        new TypeReference<List<ModelInfo>>() {}
                );
                
                // 标记失败的模型
                for (ModelInfo info : modelInfos) {
                    info.setIsFailed(failedModelIds.contains(info.getId()));
                }
                
                dto.setModelInfos(modelInfos);
                dto.setModels(modelInfos.stream().map(ModelInfo::getId).collect(Collectors.toList()));
            } catch (Exception e) {
                // 兼容旧格式（纯字符串列表）
                try {
                    List<String> modelIds = objectMapper.readValue(
                            provider.getModelsJson(),
                            new TypeReference<List<String>>() {}
                    );
                    dto.setModels(modelIds);
                    List<ModelInfo> modelInfos = modelIds.stream()
                            .map(ModelInfo::fromId)
                            .collect(Collectors.toList());
                    
                    // 标记失败的模型
                    for (ModelInfo info : modelInfos) {
                        info.setIsFailed(failedModelIds.contains(info.getId()));
                    }
                    
                    dto.setModelInfos(modelInfos);
                } catch (Exception ex) {
                    log.warn("Failed to parse models JSON", ex);
                }
            }
        }
        
        return dto;
    }
    
    public void markModelAsFailed(String providerId, String modelName) {
        ModelProvider provider = modelProviderMapper.selectById(providerId);
        if (provider == null) return;
        
        try {
            List<String> failedModels = new ArrayList<>();
            if (provider.getFailedModels() != null) {
                failedModels = objectMapper.readValue(
                        provider.getFailedModels(),
                        new TypeReference<List<String>>() {}
                );
            }
            
            if (!failedModels.contains(modelName)) {
                failedModels.add(modelName);
                provider.setFailedModels(objectMapper.writeValueAsString(failedModels));
                modelProviderMapper.updateById(provider);
                log.info("标记模型为失败: {} - {}", providerId, modelName);
            }
        } catch (Exception e) {
            log.error("Failed to mark model as failed", e);
        }
    }
    
    /**
     * 清除模型的失败标记
     */
    public void clearModelFailedMark(String providerId, String modelName) {
        ModelProvider provider = modelProviderMapper.selectById(providerId);
        if (provider == null) return;
        
        try {
            List<String> failedModels = new ArrayList<>();
            if (provider.getFailedModels() != null) {
                failedModels = objectMapper.readValue(
                        provider.getFailedModels(),
                        new TypeReference<List<String>>() {}
                );
            }
            
            if (failedModels.contains(modelName)) {
                failedModels.remove(modelName);
                provider.setFailedModels(objectMapper.writeValueAsString(failedModels));
                modelProviderMapper.updateById(provider);
                log.info("清除模型失败标记: {} - {}", providerId, modelName);
            }
        } catch (Exception e) {
            log.error("Failed to clear model failed mark", e);
        }
    }
}
