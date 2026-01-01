package com.ai.assistant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExportData {
    private String version = "1.0";
    private Long exportedAt;
    private List<SessionDTO> sessions;
    private List<MessageDTO> messages;
}
