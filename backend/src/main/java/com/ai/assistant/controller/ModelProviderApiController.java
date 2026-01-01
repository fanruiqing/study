package com.ai.assistant.controller;

import com.ai.assistant.dto.ModelInfo;
import com.ai.assistant.dto.ModelProviderDTO;
import com.ai.assistant.service.ModelProviderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 模型提供商 API 控制器 (使用 /api/model-providers 路径)
 */
@RestController
@RequestMapping("/api/model-providers")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174"}, allowCredentials = "true")
public class ModelProviderApiController {
    
    private final ModelProviderService modelProviderService;
    
    @GetMapping
    public ResponseEntity<List<ModelProviderDTO>> getAllProviders() {
        return ResponseEntity.ok(modelProviderService.getAllProviders());
    }
    
    @GetMapping("/active")
    public ResponseEntity<List<ModelProviderDTO>> getActiveProviders() {
        return ResponseEntity.ok(modelProviderService.getActiveProviders());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ModelProviderDTO> getProvider(@PathVariable String id) {
        return ResponseEntity.ok(modelProviderService.getProvider(id));
    }
    
    @PostMapping
    public ResponseEntity<ModelProviderDTO> createProvider(@RequestBody ModelProviderDTO dto) {
        return ResponseEntity.ok(modelProviderService.createProvider(dto));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ModelProviderDTO> updateProvider(
            @PathVariable String id,
            @RequestBody ModelProviderDTO dto) {
        return ResponseEntity.ok(modelProviderService.updateProvider(id, dto));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProvider(@PathVariable String id) {
        modelProviderService.deleteProvider(id);
        return ResponseEntity.ok().build();
    }
    
    /**
     * 获取提供商的模型列表
     */
    @GetMapping("/{id}/models")
    public ResponseEntity<List<ModelInfo>> getProviderModels(@PathVariable String id) {
        ModelProviderDTO provider = modelProviderService.getProvider(id);
        List<ModelInfo> models = provider.getModelInfos();
        // 如果没有详细信息,从模型ID列表创建基本的 ModelInfo
        if (models == null || models.isEmpty()) {
            List<String> modelIds = provider.getModels();
            if (modelIds != null && !modelIds.isEmpty()) {
                models = modelIds.stream()
                    .map(modelId -> {
                        ModelInfo info = new ModelInfo();
                        info.setId(modelId);
                        info.setName(modelId);
                        return info;
                    })
                    .toList();
            }
        }
        return ResponseEntity.ok(models != null ? models : List.of());
    }
    
    /**
     * 测试已保存的提供商连接
     */
    @PostMapping("/{id}/test")
    public ResponseEntity<Map<String, Object>> testConnection(@PathVariable String id) {
        return ResponseEntity.ok(modelProviderService.testConnection(id));
    }
    
    /**
     * 测试单个模型
     */
    @PostMapping("/{id}/test-model")
    public ResponseEntity<Map<String, Object>> testSingleModel(
            @PathVariable String id,
            @RequestBody Map<String, String> request) {
        String modelId = request.get("modelId");
        return ResponseEntity.ok(modelProviderService.testSingleModel(id, modelId));
    }
    
    /**
     * 清除模型的失败标记
     */
    @PostMapping("/{id}/clear-failed-mark")
    public ResponseEntity<Void> clearModelFailedMark(
            @PathVariable String id,
            @RequestBody Map<String, String> request) {
        String modelId = request.get("modelId");
        modelProviderService.clearModelFailedMark(id, modelId);
        return ResponseEntity.ok().build();
    }
    
    /**
     * 刷新提供商的模型列表
     */
    @PostMapping("/{id}/refresh-models")
    public ResponseEntity<ModelProviderDTO> refreshModels(@PathVariable String id) {
        return ResponseEntity.ok(modelProviderService.refreshModels(id));
    }
    
    /**
     * 获取提供商的嵌入模型列表
     */
    @GetMapping("/{id}/embedding-models")
    public ResponseEntity<List<ModelInfo>> getEmbeddingModels(@PathVariable String id) {
        ModelProviderDTO provider = modelProviderService.getProvider(id);
        List<ModelInfo> allModels = provider.getModelInfos();
        
        // 如果没有详细信息,从模型ID列表创建基本的 ModelInfo
        if (allModels == null || allModels.isEmpty()) {
            List<String> modelIds = provider.getModels();
            if (modelIds != null && !modelIds.isEmpty()) {
                allModels = modelIds.stream()
                    .map(modelId -> {
                        ModelInfo info = new ModelInfo();
                        info.setId(modelId);
                        info.setName(modelId);
                        return info;
                    })
                    .toList();
            }
        }
        
        // 筛选嵌入模型
        List<ModelInfo> embeddingModels = allModels != null ? allModels.stream()
            .filter(model -> isEmbeddingModel(model.getId(), model.getName()))
            .toList() : List.of();
            
        return ResponseEntity.ok(embeddingModels);
    }
    
    /**
     * 判断是否为嵌入模型
     */
    private boolean isEmbeddingModel(String modelId, String modelName) {
        if (modelId == null && modelName == null) {
            return false;
        }
        
        String id = (modelId != null ? modelId : "").toLowerCase();
        String name = (modelName != null ? modelName : "").toLowerCase();
        
        // 常见的嵌入模型关键词
        String[] keywords = {
            "embedding", "embed", "bge", "gte", "m3e", 
            "text2vec", "sentence", "e5", "instructor", 
            "multilingual-e5", "paraphrase", "distiluse"
        };
        
        for (String keyword : keywords) {
            if (id.contains(keyword) || name.contains(keyword)) {
                return true;
            }
        }
        
        return false;
    }
}
