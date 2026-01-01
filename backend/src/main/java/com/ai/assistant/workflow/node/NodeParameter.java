package com.ai.assistant.workflow.node;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NodeParameter {
    private String name;
    private String label;
    private String type;  // string, number, boolean, select, textarea, etc.
    private String description;
    private Boolean required;
    private Object defaultValue;
    private Object options;  // For select type
}
