package com.ai.assistant.dto;

import com.ai.assistant.entity.Session;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionDTO {
    private String id;
    private String title;
    private Long createdAt;
    private Long updatedAt;
    private String modelId;
    private Integer messageCount;
    
    public static SessionDTO fromEntity(Session session, int messageCount) {
        SessionDTO dto = new SessionDTO();
        dto.setId(session.getId());
        dto.setTitle(session.getTitle());
        dto.setCreatedAt(session.getCreatedAt());
        dto.setUpdatedAt(session.getUpdatedAt());
        dto.setModelId(session.getModelId());
        dto.setMessageCount(messageCount);
        return dto;
    }
    
    public Session toEntity() {
        Session session = new Session();
        session.setId(this.id != null ? this.id : UUID.randomUUID().toString());
        session.setTitle(this.title != null ? this.title : "新对话");
        long now = System.currentTimeMillis();
        session.setCreatedAt(this.createdAt != null ? this.createdAt : now);
        session.setUpdatedAt(this.updatedAt != null ? this.updatedAt : now);
        session.setModelId(this.modelId);
        return session;
    }
}
