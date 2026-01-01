package com.ai.assistant.dto;

import com.ai.assistant.entity.Message;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageDTO {
    private String id;
    private String sessionId;
    private String role;
    private String content;
    private Long timestamp;
    private String modelId;
    private Integer rating;
    private Map<String, Object> metadata;
    private List<AttachmentDTO> attachments; // 附件列表
    private String thinking; // 思考过程（从 metadata 中提取）
    
    private static final ObjectMapper mapper = new ObjectMapper();
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttachmentDTO {
        private String type; // image, file
        private String name; // 文件名
        private String url;  // 文件URL或Base64
        private String mimeType; // MIME类型
        private Long size; // 文件大小
    }
    
    public static MessageDTO fromEntity(Message message) {
        MessageDTO dto = new MessageDTO();
        dto.setId(message.getId());
        dto.setSessionId(message.getSessionId());
        dto.setRole(message.getRole() != null ? message.getRole().toLowerCase() : null);
        dto.setContent(message.getContent());
        dto.setTimestamp(message.getTimestamp());
        dto.setModelId(message.getModelId());
        dto.setRating(message.getRating());
        
        Map<String, Object> metadata = parseMetadata(message.getMetadata());
        dto.setMetadata(metadata);
        
        // 从 metadata 中提取附件
        if (metadata != null && metadata.containsKey("attachments")) {
            try {
                String attachmentsJson = mapper.writeValueAsString(metadata.get("attachments"));
                List<AttachmentDTO> attachments = mapper.readValue(
                    attachmentsJson, 
                    new TypeReference<List<AttachmentDTO>>() {}
                );
                dto.setAttachments(attachments);
            } catch (JsonProcessingException e) {
                // 忽略解析错误
            }
        }
        
        // 从 metadata 中提取思考内容
        if (metadata != null && metadata.containsKey("thinking")) {
            dto.setThinking((String) metadata.get("thinking"));
        }
        
        return dto;
    }
    
    public Message toEntity() {
        Message message = new Message();
        message.setId(this.id != null ? this.id : UUID.randomUUID().toString());
        message.setSessionId(this.sessionId);
        message.setRole(this.role != null ? this.role.toUpperCase() : "USER");
        message.setContent(this.content);
        message.setTimestamp(this.timestamp != null ? this.timestamp : System.currentTimeMillis());
        message.setModelId(this.modelId);
        message.setRating(this.rating);
        
        // 将附件和思考内容存入 metadata
        Map<String, Object> metadata = this.metadata;
        if (metadata == null) {
            metadata = new java.util.HashMap<>();
        }
        
        if (this.attachments != null && !this.attachments.isEmpty()) {
            metadata.put("attachments", this.attachments);
        }
        
        if (this.thinking != null && !this.thinking.isEmpty()) {
            metadata.put("thinking", this.thinking);
        }
        
        message.setMetadata(serializeMetadata(metadata));
        return message;
    }
    
    private static Map<String, Object> parseMetadata(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return mapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            return null;
        }
    }
    
    private static String serializeMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        try {
            return mapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
