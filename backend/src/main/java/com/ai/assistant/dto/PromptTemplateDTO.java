package com.ai.assistant.dto;

import com.ai.assistant.entity.PromptTemplate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PromptTemplateDTO {
    private String id;
    private String title;
    private String content;
    private String category;
    private Long createdAt;
    private Long updatedAt;
    
    public static PromptTemplateDTO fromEntity(PromptTemplate template) {
        PromptTemplateDTO dto = new PromptTemplateDTO();
        dto.setId(template.getId());
        dto.setTitle(template.getTitle());
        dto.setContent(template.getContent());
        dto.setCategory(template.getCategory());
        dto.setCreatedAt(template.getCreatedAt());
        dto.setUpdatedAt(template.getUpdatedAt());
        return dto;
    }
    
    public PromptTemplate toEntity() {
        PromptTemplate template = new PromptTemplate();
        template.setId(this.id != null ? this.id : UUID.randomUUID().toString());
        template.setTitle(this.title);
        template.setContent(this.content);
        template.setCategory(this.category);
        long now = System.currentTimeMillis();
        template.setCreatedAt(this.createdAt != null ? this.createdAt : now);
        template.setUpdatedAt(this.updatedAt != null ? this.updatedAt : now);
        return template;
    }
}
