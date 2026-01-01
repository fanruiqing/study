package com.ai.assistant.service;

import com.ai.assistant.dto.WorkflowDTO;
import com.ai.assistant.entity.Workflow;
import com.ai.assistant.mapper.WorkflowMapper;
import com.ai.assistant.workflow.model.WorkflowDefinition;
import com.ai.assistant.workflow.validator.WorkflowValidator;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkflowService {
    
    private final WorkflowMapper workflowMapper;
    private final WorkflowValidator workflowValidator;
    private final ObjectMapper objectMapper;
    
    public Workflow create(WorkflowDTO dto) {
        Workflow workflow = new Workflow();
        workflow.setName(dto.getName());
        workflow.setDescription(dto.getDescription());
        workflow.setDefinition(dto.getDefinition());
        workflow.setStatus("DRAFT");
        workflow.setVersion("1.0.0");
        workflow.setCreatedAt(System.currentTimeMillis());
        workflow.setUpdatedAt(System.currentTimeMillis());
        
        workflowMapper.insert(workflow);
        return workflow;
    }
    
    public Workflow update(String id, WorkflowDTO dto) {
        Workflow workflow = workflowMapper.selectById(id);
        if (workflow == null) {
            throw new RuntimeException("Workflow not found");
        }
        
        workflow.setName(dto.getName());
        workflow.setDescription(dto.getDescription());
        workflow.setDefinition(dto.getDefinition());
        workflow.setUpdatedAt(System.currentTimeMillis());
        
        workflowMapper.updateById(workflow);
        return workflow;
    }
    
    public void delete(String id) {
        workflowMapper.deleteById(id);
    }
    
    public Workflow getById(String id) {
        return workflowMapper.selectById(id);
    }
    
    public List<Workflow> list() {
        return workflowMapper.selectList(new QueryWrapper<>());
    }
    
    public Workflow publish(String id) {
        Workflow workflow = workflowMapper.selectById(id);
        if (workflow == null) {
            throw new RuntimeException("Workflow not found");
        }
        
        // 验证工作流
        try {
            WorkflowDefinition definition = objectMapper.readValue(
                workflow.getDefinition(), WorkflowDefinition.class);
            WorkflowValidator.ValidationResult result = workflowValidator.validate(definition);
            
            if (!result.isValid()) {
                throw new RuntimeException("Workflow validation failed: " + result.getErrors());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to validate workflow", e);
        }
        
        workflow.setStatus("PUBLISHED");
        workflow.setApiEndpoint("/api/workflows/published/" + id);
        workflow.setUpdatedAt(System.currentTimeMillis());
        
        workflowMapper.updateById(workflow);
        return workflow;
    }
    
    public void unpublish(String id) {
        Workflow workflow = workflowMapper.selectById(id);
        if (workflow == null) {
            throw new RuntimeException("Workflow not found");
        }
        
        workflow.setStatus("DRAFT");
        workflow.setApiEndpoint(null);
        workflow.setUpdatedAt(System.currentTimeMillis());
        
        workflowMapper.updateById(workflow);
    }
    
    public WorkflowValidator.ValidationResult validate(String id) {
        Workflow workflow = workflowMapper.selectById(id);
        if (workflow == null) {
            throw new RuntimeException("Workflow not found");
        }
        
        try {
            WorkflowDefinition definition = objectMapper.readValue(
                workflow.getDefinition(), WorkflowDefinition.class);
            return workflowValidator.validate(definition);
        } catch (Exception e) {
            WorkflowValidator.ValidationResult result = new WorkflowValidator.ValidationResult();
            result.addError("Failed to parse workflow definition: " + e.getMessage());
            return result;
        }
    }
}
