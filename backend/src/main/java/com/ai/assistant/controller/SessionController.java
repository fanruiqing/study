package com.ai.assistant.controller;

import com.ai.assistant.dto.SessionDTO;
import com.ai.assistant.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {
    
    private final SessionService sessionService;
    
    @GetMapping
    public ResponseEntity<List<SessionDTO>> getAllSessions() {
        return ResponseEntity.ok(sessionService.getAllSessions());
    }
    
    @PostMapping
    public ResponseEntity<SessionDTO> createSession() {
        return ResponseEntity.ok(sessionService.createSession());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<SessionDTO> getSession(@PathVariable String id) {
        return ResponseEntity.ok(sessionService.getSession(id));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<SessionDTO> updateSession(
            @PathVariable String id,
            @RequestBody SessionDTO dto) {
        return ResponseEntity.ok(sessionService.updateSession(id, dto));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSession(@PathVariable String id) {
        sessionService.deleteSession(id);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<SessionDTO>> searchSessions(@RequestParam String query) {
        return ResponseEntity.ok(sessionService.searchSessions(query));
    }
}
