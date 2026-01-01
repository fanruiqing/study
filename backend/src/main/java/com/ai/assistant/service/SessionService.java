package com.ai.assistant.service;

import com.ai.assistant.dto.SessionDTO;
import com.ai.assistant.entity.Session;
import com.ai.assistant.mapper.MessageMapper;
import com.ai.assistant.mapper.SessionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SessionService {
    
    private final SessionMapper sessionMapper;
    private final MessageMapper messageMapper;
    
    public List<SessionDTO> getAllSessions() {
        return sessionMapper.findAllByOrderByUpdatedAtDesc()
                .stream()
                .map(session -> SessionDTO.fromEntity(session, 
                        (int) messageMapper.countBySessionId(session.getId())))
                .collect(Collectors.toList());
    }
    
    public SessionDTO createSession() {
        Session session = new Session();
        session.setId(UUID.randomUUID().toString());
        session.setTitle("新对话");
        long now = System.currentTimeMillis();
        session.setCreatedAt(now);
        session.setUpdatedAt(now);
        
        sessionMapper.insert(session);
        return SessionDTO.fromEntity(session, 0);
    }
    
    public SessionDTO getSession(String id) {
        Session session = sessionMapper.selectById(id);
        if (session == null) {
            throw new RuntimeException("Session not found: " + id);
        }
        int messageCount = (int) messageMapper.countBySessionId(id);
        return SessionDTO.fromEntity(session, messageCount);
    }
    
    public SessionDTO updateSession(String id, SessionDTO dto) {
        Session session = sessionMapper.selectById(id);
        if (session == null) {
            throw new RuntimeException("Session not found: " + id);
        }
        
        if (dto.getTitle() != null) {
            session.setTitle(dto.getTitle());
        }
        if (dto.getModelId() != null) {
            session.setModelId(dto.getModelId());
        }
        session.setUpdatedAt(System.currentTimeMillis());
        
        sessionMapper.updateById(session);
        int messageCount = (int) messageMapper.countBySessionId(id);
        return SessionDTO.fromEntity(session, messageCount);
    }
    
    @Transactional
    public void deleteSession(String id) {
        messageMapper.deleteBySessionId(id);
        sessionMapper.deleteById(id);
    }
    
    public List<SessionDTO> searchSessions(String query) {
        List<Session> sessions = sessionMapper.searchByTitle(query);
        return sessions.stream()
                .map(session -> SessionDTO.fromEntity(session,
                        (int) messageMapper.countBySessionId(session.getId())))
                .collect(Collectors.toList());
    }
    
    public void updateSessionTimestamp(String sessionId) {
        Session session = sessionMapper.selectById(sessionId);
        if (session != null) {
            session.setUpdatedAt(System.currentTimeMillis());
            sessionMapper.updateById(session);
        }
    }
}
