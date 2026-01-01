package com.ai.assistant.workflow.node;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class NodeMetadata {
    private String type;
    private String name;
    private String description;
    private String icon;
    private String category;
    private List<NodeParameter> parameters;
    private List<String> inputPorts;
    private List<String> outputPorts;
}
