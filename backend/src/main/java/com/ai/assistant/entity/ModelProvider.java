package com.ai.assistant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("model_providers")
public class ModelProvider {
    
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    
    private String name;
    
    private String type;
    
    @TableField("api_key")
    private String apiKey;
    
    @TableField("base_url")
    private String baseUrl;
    
    @TableField("model_name")
    private String modelName;
    
    @TableField("models_json")
    private String modelsJson;
    
    @TableField("is_active")
    private Boolean isActive = true;
    
    private Double temperature = 0.7;
    
    @TableField("max_tokens")
    private Integer maxTokens = 2048;
    
    @TableField("failed_models")
    private String failedModels;  // JSON array of failed model IDs
}
