package com.ai.assistant.workflow.execution;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class NodeOutput {
    private Map<String, Object> data;
    private NodeStatus status;
    private String error;
}
