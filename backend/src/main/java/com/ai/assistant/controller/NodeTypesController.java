package com.ai.assistant.controller;

import com.ai.assistant.workflow.node.NodeMetadata;
import com.ai.assistant.workflow.registry.NodeRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/node-types")
@RequiredArgsConstructor
public class NodeTypesController {
    
    private final NodeRegistry nodeRegistry;
    
    @GetMapping
    public ResponseEntity<List<NodeMetadata>> list() {
        return ResponseEntity.ok(nodeRegistry.getAllNodeTypes());
    }
    
    @GetMapping("/{type}")
    public ResponseEntity<NodeMetadata> getByType(@PathVariable String type) {
        var node = nodeRegistry.getNode(type);
        if (node == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(node.getMetadata());
    }
}
