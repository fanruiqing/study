package com.ai.assistant.workflow.exception;

public class NodeExecutionException extends Exception {
    public NodeExecutionException(String message) {
        super(message);
    }
    
    public NodeExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
