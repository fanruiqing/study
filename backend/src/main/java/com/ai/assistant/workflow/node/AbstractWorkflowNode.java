package com.ai.assistant.workflow.node;

import com.ai.assistant.workflow.exception.ValidationException;
import com.ai.assistant.workflow.execution.ExecutionContext;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 抽象工作流节点基类
 */
public abstract class AbstractWorkflowNode implements WorkflowNode {
    
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");
    
    /**
     * 解析变量引用
     */
    protected String resolveVariables(String template, ExecutionContext context) {
        if (template == null) {
            return null;
        }
        
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String varPath = matcher.group(1).trim();
            Object value = resolveVariablePath(varPath, context);
            matcher.appendReplacement(result, value != null ? value.toString() : "");
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    /**
     * 解析变量路径（如 "input.question" 或 "kb-search.context"）
     */
    protected Object resolveVariablePath(String path, ExecutionContext context) {
        String[] parts = path.split("\\.");
        
        if (parts.length == 1) {
            return context.getVariable(parts[0]);
        } else if (parts.length == 2) {
            Object nodeOutput = context.getNodeOutput(parts[0]);
            if (nodeOutput instanceof Map) {
                return ((Map<?, ?>) nodeOutput).get(parts[1]);
            }
        }
        
        return null;
    }
    
    /**
     * 验证必需配置项
     */
    protected void validateRequiredConfig(Map<String, Object> config, String... requiredKeys) throws ValidationException {
        if (config == null) {
            throw new ValidationException("Configuration is required");
        }
        
        for (String key : requiredKeys) {
            if (!config.containsKey(key) || config.get(key) == null) {
                throw new ValidationException("Required configuration '" + key + "' is missing");
            }
        }
    }
}
