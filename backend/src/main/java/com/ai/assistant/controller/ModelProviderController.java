package com.ai.assistant.controller;

import com.ai.assistant.dto.ModelInfo;
import com.ai.assistant.dto.ModelProviderDTO;
import com.ai.assistant.service.ModelProviderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/models")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174"}, allowCredentials = "true")
public class ModelProviderController {
    
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
     * 测试已保存的提供商连接
     */
    @PostMapping("/{id}/test")
    public ResponseEntity<Map<String, Object>> testConnection(@PathVariable String id) {
        return ResponseEntity.ok(modelProviderService.testConnection(id));
    }
    
    /**
     * 测试连接（通过 URL 和 API Key）
     */
    @PostMapping("/test-connection")
    public ResponseEntity<Map<String, Object>> testConnectionWithUrl(@RequestBody Map<String, String> request) {
        String baseUrl = request.get("baseUrl");
        String apiKey = request.get("apiKey");
        
        if (baseUrl == null || apiKey == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "请提供 baseUrl 和 apiKey"
            ));
        }
        
        return ResponseEntity.ok(modelProviderService.testConnectionWithUrl(baseUrl, apiKey));
    }
    
    /**
     * 获取模型列表（带详细信息）
     */
    @PostMapping("/fetch-models")
    public ResponseEntity<Map<String, Object>> fetchModels(@RequestBody Map<String, String> request) {
        String baseUrl = request.get("baseUrl");
        String apiKey = request.get("apiKey");
        
        if (baseUrl == null || apiKey == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "请提供 baseUrl 和 apiKey"
            ));
        }
        
        try {
            List<ModelInfo> models = modelProviderService.fetchModelsWithInfo(baseUrl, apiKey);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "models", models
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
    
    /**
     * 获取分组后的模型列表
     */
    @PostMapping("/fetch-models-grouped")
    public ResponseEntity<Map<String, Object>> fetchModelsGrouped(@RequestBody Map<String, String> request) {
        String baseUrl = request.get("baseUrl");
        String apiKey = request.get("apiKey");
        
        if (baseUrl == null || apiKey == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "请提供 baseUrl 和 apiKey"
            ));
        }
        
        try {
            Map<String, List<ModelInfo>> grouped = modelProviderService.fetchModelsGrouped(baseUrl, apiKey);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "groups", grouped
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
    
    /**
     * 刷新提供商的模型列表
     */
    @PostMapping("/{id}/refresh-models")
    public ResponseEntity<ModelProviderDTO> refreshModels(@PathVariable String id) {
        return ResponseEntity.ok(modelProviderService.refreshModels(id));
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
}
