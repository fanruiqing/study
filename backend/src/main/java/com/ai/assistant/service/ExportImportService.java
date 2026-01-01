package com.ai.assistant.service;

import com.ai.assistant.dto.ExportData;
import com.ai.assistant.dto.MessageDTO;
import com.ai.assistant.dto.SessionDTO;
import com.ai.assistant.entity.Message;
import com.ai.assistant.entity.Session;
import com.ai.assistant.mapper.MessageMapper;
import com.ai.assistant.mapper.SessionMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExportImportService {
    
    private final SessionMapper sessionMapper;
    private final MessageMapper messageMapper;
    private final ObjectMapper objectMapper;
    
    public ExportData exportAll() {
        List<Session> sessions = sessionMapper.selectList(null);
        List<Message> messages = messageMapper.selectList(null);
        
        List<SessionDTO> sessionDTOs = sessions.stream()
                .map(s -> SessionDTO.fromEntity(s, (int) messageMapper.countBySessionId(s.getId())))
                .collect(Collectors.toList());
        
        List<MessageDTO> messageDTOs = messages.stream()
                .map(MessageDTO::fromEntity)
                .collect(Collectors.toList());
        
        ExportData data = new ExportData();
        data.setVersion("1.0");
        data.setExportedAt(System.currentTimeMillis());
        data.setSessions(sessionDTOs);
        data.setMessages(messageDTOs);
        
        return data;
    }
    
    public String exportToJson() {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(exportAll());
        } catch (Exception e) {
            throw new RuntimeException("Failed to export data", e);
        }
    }
    
    public String exportToMarkdown() {
        StringBuilder sb = new StringBuilder();
        sb.append("# AI Assistant 对话导出\n\n");
        sb.append("导出时间: ").append(new java.util.Date()).append("\n\n");
        
        List<Session> sessions = sessionMapper.findAllByOrderByUpdatedAtDesc();
        
        for (Session session : sessions) {
            sb.append("## ").append(session.getTitle()).append("\n\n");
            
            List<Message> messages = messageMapper.findBySessionIdOrderByTimestampAsc(session.getId());
            for (Message message : messages) {
                String role = message.getRole().toLowerCase();
                sb.append("### ").append(role.equals("user") ? "👤 用户" : "🤖 助手").append("\n\n");
                sb.append(message.getContent()).append("\n\n");
            }
            sb.append("---\n\n");
        }
        
        return sb.toString();
    }
    
    @Transactional
    public void importFromJson(String json) {
        try {
            ExportData data = objectMapper.readValue(json, ExportData.class);
            
            // Import sessions
            for (SessionDTO sessionDTO : data.getSessions()) {
                Session session = sessionDTO.toEntity();
                sessionMapper.insert(session);
            }
            
            // Import messages
            for (MessageDTO messageDTO : data.getMessages()) {
                Message message = messageDTO.toEntity();
                messageMapper.insert(message);
            }
            
            log.info("Imported {} sessions and {} messages", 
                    data.getSessions().size(), data.getMessages().size());
        } catch (Exception e) {
            throw new RuntimeException("Failed to import data", e);
        }
    }
}
