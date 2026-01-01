package com.ai.assistant.workflow.execution;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class NodeInput {
    private Map<String, Object> data;
    private Map<String, Object> config;
}
