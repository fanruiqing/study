package com.ai.assistant.service;

import com.ai.assistant.dto.PromptTemplateDTO;
import com.ai.assistant.entity.PromptTemplate;
import com.ai.assistant.mapper.PromptTemplateMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PromptService {
    
    private final PromptTemplateMapper promptTemplateMapper;
    
    public List<PromptTemplateDTO> getAllTemplates() {
        return promptTemplateMapper.selectList(null)
                .stream()
                .map(PromptTemplateDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    public Map<String, List<PromptTemplateDTO>> getTemplatesGroupedByCategory() {
        return promptTemplateMapper.selectList(null)
                .stream()
                .map(PromptTemplateDTO::fromEntity)
                .collect(Collectors.groupingBy(
                        dto -> dto.getCategory() != null ? dto.getCategory() : "未分类"
                ));
    }
    
    public List<String> getAllCategories() {
        return promptTemplateMapper.findAllCategories();
    }
    
    public PromptTemplateDTO getTemplate(String id) {
        PromptTemplate template = promptTemplateMapper.selectById(id);
        if (template == null) {
            throw new RuntimeException("Template not found: " + id);
        }
        return PromptTemplateDTO.fromEntity(template);
    }
    
    public PromptTemplateDTO createTemplate(PromptTemplateDTO dto) {
        PromptTemplate template = dto.toEntity();
        if (template.getId() == null) {
            template.setId(UUID.randomUUID().toString());
        }
        long now = System.currentTimeMillis();
        template.setCreatedAt(now);
        template.setUpdatedAt(now);
        
        promptTemplateMapper.insert(template);
        return PromptTemplateDTO.fromEntity(template);
    }
    
    public PromptTemplateDTO updateTemplate(String id, PromptTemplateDTO dto) {
        PromptTemplate template = promptTemplateMapper.selectById(id);
        if (template == null) {
            throw new RuntimeException("Template not found: " + id);
        }
        
        if (dto.getTitle() != null) {
            template.setTitle(dto.getTitle());
        }
        if (dto.getContent() != null) {
            template.setContent(dto.getContent());
        }
        if (dto.getCategory() != null) {
            template.setCategory(dto.getCategory());
        }
        template.setUpdatedAt(System.currentTimeMillis());
        
        promptTemplateMapper.updateById(template);
        return PromptTemplateDTO.fromEntity(template);
    }
    
    public void deleteTemplate(String id) {
        promptTemplateMapper.deleteById(id);
    }
    
    public List<PromptTemplateDTO> searchTemplates(String query) {
        return promptTemplateMapper.search(query)
                .stream()
                .map(PromptTemplateDTO::fromEntity)
                .collect(Collectors.toList());
    }
}
