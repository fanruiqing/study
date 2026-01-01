package com.ai.assistant.dto;

import com.ai.assistant.entity.ModelProvider;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModelProviderDTO {
    private String id;
    private String name;
    private String type;
    private String apiKey;
    private String baseUrl;
    private String modelName;
    private Boolean isActive;
    private Double temperature;
    private Integer maxTokens;
    private List<String> models;           // 模型ID列表（兼容）
    private List<ModelInfo> modelInfos;    // 详细模型信息列表
    
    public static ModelProviderDTO fromEntity(ModelProvider provider) {
        ModelProviderDTO dto = new ModelProviderDTO();
        dto.setId(provider.getId());
        dto.setName(provider.getName());
        dto.setType(provider.getType() != null ? provider.getType().toLowerCase() : null);
        dto.setApiKey(maskApiKey(provider.getApiKey()));
        dto.setBaseUrl(provider.getBaseUrl());
        dto.setModelName(provider.getModelName());
        dto.setIsActive(provider.getIsActive());
        dto.setTemperature(provider.getTemperature());
        dto.setMaxTokens(provider.getMaxTokens());
        return dto;
    }
    
    public ModelProvider toEntity() {
        ModelProvider provider = new ModelProvider();
        provider.setId(this.id != null ? this.id : UUID.randomUUID().toString());
        provider.setName(this.name);
        provider.setType(this.type != null ? this.type.toUpperCase() : "OPENAI");
        provider.setApiKey(this.apiKey);
        provider.setBaseUrl(this.baseUrl);
        provider.setModelName(this.modelName);
        provider.setIsActive(this.isActive != null ? this.isActive : true);
        provider.setTemperature(this.temperature != null ? this.temperature : 0.7);
        provider.setMaxTokens(this.maxTokens != null ? this.maxTokens : 2048);
        return provider;
    }
    
    private static String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() < 8) {
            return "****";
        }
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }
}
