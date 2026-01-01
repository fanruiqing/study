package com.ai.assistant.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException e, HttpServletRequest request) {
        // 如果是SSE请求，不处理异常（让它自然传播）
        if (isSSERequest(request)) {
            log.error("SSE请求发生运行时异常: {}", e.getMessage());
            throw e;
        }
        
        log.error("Runtime exception", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "error", "INTERNAL_ERROR",
                        "message", e.getMessage(),
                        "retryable", true
                ));
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException e, HttpServletRequest request) {
        // 如果是SSE请求，不处理异常
        if (isSSERequest(request)) {
            log.warn("SSE请求发生参数验证异常: {}", e.getMessage());
            throw e;
        }
        
        log.warn("Validation error", e);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "error", "VALIDATION_ERROR",
                        "message", e.getMessage(),
                        "retryable", false
                ));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception e, HttpServletRequest request) {
        // 如果是SSE请求，不处理异常
        if (isSSERequest(request)) {
            log.error("SSE请求发生未知异常: {}", e.getMessage());
            throw new RuntimeException(e);
        }
        
        log.error("Unexpected exception", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "error", "UNKNOWN_ERROR",
                        "message", "服务器内部错误",
                        "retryable", true
                ));
    }
    
    /**
     * 判断是否是SSE请求
     */
    private boolean isSSERequest(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains("text/event-stream");
    }
}
