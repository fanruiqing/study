package com.ai.assistant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModelInfo {
    private String id;
    private String name;
    private String group;           // 分组：deepseek, gpt, claude, embedding, etc.
    private String type;            // 类型：chat, embedding, vision, tool
    private Boolean supportTools;   // 是否支持工具调用/MCP
    private Boolean supportVision;  // 是否支持视觉
    private Long contextLength;     // 上下文长度
    private Boolean isFailed;       // 是否调用失败过
    
    public static ModelInfo fromId(String modelId) {
        ModelInfo info = new ModelInfo();
        info.setId(modelId);
        info.setName(modelId);
        info.setGroup(detectGroup(modelId));
        info.setType(detectType(modelId));
        info.setSupportTools(detectToolSupport(modelId));
        info.setSupportVision(detectVisionSupport(modelId));
        info.setIsFailed(false);
        return info;
    }
    
    private static String detectGroup(String modelId) {
        String lower = modelId.toLowerCase();
        if (lower.contains("deepseek")) return "deepseek";
        if (lower.contains("gpt-4")) return "gpt-4";
        if (lower.contains("gpt-3")) return "gpt-3.5";
        if (lower.contains("claude")) return "claude";
        if (lower.contains("qwen")) return "qwen";
        if (lower.contains("llama")) return "llama";
        if (lower.contains("mistral")) return "mistral";
        if (lower.contains("gemini")) return "gemini";
        if (lower.contains("embedding") || lower.contains("embed") || lower.contains("bge")) return "embedding";
        if (lower.contains("whisper")) return "whisper";
        if (lower.contains("dall") || lower.contains("image")) return "image";
        if (lower.contains("tts") || lower.contains("speech")) return "speech";
        if (lower.contains("nvidia") || lower.contains("nv-")) return "nvidia";
        return "other";
    }
    
    private static String detectType(String modelId) {
        String lower = modelId.toLowerCase();
        if (lower.contains("embedding") || lower.contains("embed") || lower.contains("bge")) return "embedding";
        if (lower.contains("whisper") || lower.contains("transcription")) return "audio";
        if (lower.contains("dall") || lower.contains("image-generation")) return "image";
        if (lower.contains("tts") || lower.contains("speech")) return "tts";
        if (lower.contains("vision") || lower.contains("4o") || lower.contains("4-turbo")) return "vision";
        return "chat";
    }
    
    private static Boolean detectToolSupport(String modelId) {
        String lower = modelId.toLowerCase();
        // 支持工具调用的模型
        if (lower.contains("gpt-4") || lower.contains("gpt-3.5-turbo")) return true;
        if (lower.contains("claude-3")) return true;
        if (lower.contains("deepseek-chat") || lower.contains("deepseek-coder")) return true;
        if (lower.contains("qwen") && !lower.contains("vl")) return true;
        if (lower.contains("mistral") && lower.contains("large")) return true;
        return false;
    }
    
    private static Boolean detectVisionSupport(String modelId) {
        String lower = modelId.toLowerCase();
        if (lower.contains("vision")) return true;
        if (lower.contains("gpt-4o") || lower.contains("gpt-4-turbo")) return true;
        if (lower.contains("claude-3")) return true;
        if (lower.contains("qwen-vl") || lower.contains("qwen2-vl")) return true;
        if (lower.contains("gemini")) return true;
        return false;
    }
}
