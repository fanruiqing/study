package com.ai.assistant.controller;

import com.ai.assistant.dto.MessageDTO;
import com.ai.assistant.dto.SendMessageRequest;
import com.ai.assistant.service.ChatService;
import com.ai.assistant.service.EnhancedChatService;
import com.ai.assistant.service.FileService;
import com.ai.assistant.service.DocumentExtractorService;
import com.ai.assistant.service.KnowledgeBaseChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {
    
    private final ChatService chatService;
    private final EnhancedChatService enhancedChatService;
    private final FileService fileService;
    private final DocumentExtractorService documentExtractorService;
    private final KnowledgeBaseChatService knowledgeBaseChatService;
    
    @GetMapping("/messages/{sessionId}")
    public ResponseEntity<List<MessageDTO>> getMessages(@PathVariable String sessionId) {
        return ResponseEntity.ok(chatService.getMessages(sessionId));
    }
    
    @PostMapping("/messages")
    public ResponseEntity<MessageDTO> saveMessage(@RequestBody MessageDTO dto) {
        return ResponseEntity.ok(chatService.saveMessage(dto));
    }
    
    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<Void> deleteMessage(@PathVariable String messageId) {
        chatService.deleteMessage(messageId);
        return ResponseEntity.ok().build();
    }
    
    /**
     * 上传文件并提取内容
     */
    @PostMapping("/upload-document")
    public ResponseEntity<Map<String, String>> uploadDocument(@RequestParam("file") MultipartFile file) {
        try {
            // 提取文档内容
            String content = documentExtractorService.extractContent(file);
            
            // 同时保存文件
            String filename = fileService.uploadFile(file);
            
            return ResponseEntity.ok(Map.of(
                "success", "true",
                "filename", filename,
                "content", content,
                "message", "文档上传并提取成功"
            ));
        } catch (IOException e) {
            log.error("文档处理失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", "false",
                "message", "文档处理失败: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 上传文件
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            String filename = fileService.uploadFile(file);
            return ResponseEntity.ok(Map.of(
                "success", "true",
                "filename", filename,
                "message", "文件上传成功"
            ));
        } catch (IOException e) {
            log.error("文件上传失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", "false",
                "message", "文件上传失败: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 上传图片并转换为 Base64
     */
    @PostMapping("/upload-image")
    public ResponseEntity<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            String base64 = fileService.uploadImageAsBase64(file);
            return ResponseEntity.ok(Map.of(
                "success", "true",
                "base64", base64,
                "message", "图片上传成功"
            ));
        } catch (IOException e) {
            log.error("图片上传失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", "false",
                "message", "图片上传失败: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 流式聊天（支持附件）
     */
    @PostMapping(value = "/stream-with-attachments", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChatWithAttachments(
            @RequestParam String sessionId,
            @RequestParam String content,
            @RequestParam String modelId,
            @RequestParam(required = false) MultipartFile[] images) {
        
        log.info("=== 收到流式聊天请求（带附件） ===");
        log.info("Session ID: {}", sessionId);
        log.info("Model ID: {}", modelId);
        log.info("Content: {}", content.length() > 100 ? content.substring(0, 100) + "..." : content);
        log.info("Images: {}", images != null ? images.length : 0);
        
        SseEmitter emitter = new SseEmitter(300000L); // 5 minutes timeout
        
        emitter.onCompletion(() -> log.info("✓ SSE 连接完成 - sessionId: {}", sessionId));
        emitter.onTimeout(() -> {
            log.warn("⚠ SSE 连接超时 - sessionId: {}", sessionId);
            emitter.complete();
        });
        emitter.onError(e -> log.error("✗ SSE 连接错误 - sessionId: {}, error: {}", sessionId, e.getMessage()));
        
        // 处理图片附件
        List<MessageDTO.AttachmentDTO> attachments = new ArrayList<>();
        if (images != null && images.length > 0) {
            for (MultipartFile image : images) {
                try {
                    String base64 = fileService.uploadImageAsBase64(image);
                    MessageDTO.AttachmentDTO attachment = new MessageDTO.AttachmentDTO();
                    attachment.setType("image");
                    attachment.setName(image.getOriginalFilename());
                    attachment.setUrl(base64);
                    attachment.setMimeType(image.getContentType());
                    attachment.setSize(image.getSize());
                    attachments.add(attachment);
                    log.info("✓ 图片已转换为 Base64: {}", image.getOriginalFilename());
                } catch (IOException e) {
                    log.error("图片处理失败: {}", image.getOriginalFilename(), e);
                }
            }
        }
        
        chatService.streamChat(sessionId, content, modelId, attachments)
                .doOnSubscribe(s -> log.info("开始订阅流式响应..."))
                .doOnNext(chunk -> {
                    if (chunk != null && !chunk.isEmpty()) {
                        log.debug("→ 发送数据块: {} 字符", chunk.length());
                    }
                })
                .doOnComplete(() -> log.info("✓ 流式响应完成"))
                .doOnError(error -> log.error("✗ 流式响应错误: {}", error.getMessage(), error))
                .subscribe(
                        chunk -> {
                            try {
                                if (chunk != null && !chunk.isEmpty()) {
                                    emitter.send(SseEmitter.event().data(chunk));
                                }
                            } catch (IOException e) {
                                log.error("发送SSE数据失败", e);
                                emitter.completeWithError(e);
                            }
                        },
                        error -> {
                            log.error("订阅错误", error);
                            try {
                                emitter.send(SseEmitter.event()
                                        .name("error")
                                        .data(Map.of("message", error.getMessage())));
                            } catch (IOException ignored) {}
                            emitter.completeWithError(error);
                        },
                        () -> {
                            try {
                                emitter.send(SseEmitter.event().name("done").data(""));
                            } catch (IOException ignored) {}
                            emitter.complete();
                        }
                );
        
        return emitter;
    }
    
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(
            @RequestParam String sessionId,
            @RequestParam String content,
            @RequestParam String modelId,
            @RequestParam(required = false) String extractedContent) {
        
        log.info("=== 收到流式聊天请求 ===");
        log.info("Session ID: {}", sessionId);
        log.info("Model ID: {}", modelId);
        log.info("Content: {}", content.length() > 100 ? content.substring(0, 100) + "..." : content);
        log.info("Extracted Content: {}", extractedContent != null ? extractedContent.length() + " 字符" : "无");
        
        SseEmitter emitter = new SseEmitter(300000L); // 5 minutes timeout
        
        emitter.onCompletion(() -> log.info("✓ SSE 连接完成 - sessionId: {}", sessionId));
        emitter.onTimeout(() -> {
            log.warn("⚠ SSE 连接超时 - sessionId: {}", sessionId);
            emitter.complete();
        });
        emitter.onError(e -> log.error("✗ SSE 连接错误 - sessionId: {}, error: {}", sessionId, e.getMessage()));
        
        // 使用增强服务（支持真正的多模态）
        enhancedChatService.streamChat(sessionId, content, modelId, null, extractedContent)
                .doOnSubscribe(s -> log.info("开始订阅流式响应..."))
                .doOnNext(chunk -> {
                    if (chunk != null && !chunk.isEmpty()) {
                        log.debug("→ 发送数据块: {} 字符", chunk.length());
                    }
                })
                .doOnComplete(() -> log.info("✓ 流式响应完成"))
                .doOnError(error -> log.error("✗ 流式响应错误: {}", error.getMessage(), error))
                .subscribe(
                        chunk -> {
                            try {
                                if (chunk != null && !chunk.isEmpty()) {
                                    emitter.send(SseEmitter.event().data(chunk));
                                }
                            } catch (IOException e) {
                                log.error("发送SSE数据失败", e);
                                emitter.completeWithError(e);
                            }
                        },
                        error -> {
                            log.error("订阅错误", error);
                            try {
                                emitter.send(SseEmitter.event()
                                        .name("error")
                                        .data(Map.of("message", error.getMessage())));
                            } catch (IOException ignored) {}
                            emitter.completeWithError(error);
                        },
                        () -> {
                            try {
                                emitter.send(SseEmitter.event().name("done").data(""));
                            } catch (IOException ignored) {}
                            emitter.complete();
                        }
                );
        
        return emitter;
    }
    
    @PostMapping("/regenerate/{messageId}")
    public SseEmitter regenerateMessage(@PathVariable String messageId) {
        SseEmitter emitter = new SseEmitter(300000L);
        
        chatService.regenerateMessage(messageId)
                .subscribe(
                        chunk -> {
                            try {
                                if (chunk != null && !chunk.isEmpty()) {
                                    emitter.send(SseEmitter.event().data(chunk));
                                }
                            } catch (IOException e) {
                                emitter.completeWithError(e);
                            }
                        },
                        error -> {
                            try {
                                emitter.send(SseEmitter.event()
                                        .name("error")
                                        .data(Map.of("message", error.getMessage())));
                            } catch (IOException ignored) {}
                            emitter.completeWithError(error);
                        },
                        () -> {
                            try {
                                emitter.send(SseEmitter.event().name("done").data(""));
                            } catch (IOException ignored) {}
                            emitter.complete();
                        }
                );
        
        return emitter;
    }
    
    @PostMapping("/messages/{messageId}/rate")
    public ResponseEntity<Void> rateMessage(
            @PathVariable String messageId,
            @RequestBody Map<String, Integer> body) {
        chatService.rateMessage(messageId, body.get("rating"));
        return ResponseEntity.ok().build();
    }
    
    /**
     * 带知识库的流式对话
     */
    @GetMapping(value = "/with-knowledge", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatWithKnowledge(
            @RequestParam String sessionId,
            @RequestParam String content,
            @RequestParam String modelId) {
        
        log.info("=== 收到带知识库的对话请求 ===");
        log.info("Session ID: {}", sessionId);
        log.info("Model ID: {}", modelId);
        log.info("Content: {}", content);
        
        SseEmitter emitter = new SseEmitter(300000L);
        
        emitter.onCompletion(() -> log.info("✓ 知识库对话完成 - sessionId: {}", sessionId));
        emitter.onTimeout(() -> {
            log.warn("⚠ 知识库对话超时 - sessionId: {}", sessionId);
            emitter.complete();
        });
        emitter.onError(e -> log.error("✗ 知识库对话错误 - sessionId: {}, error: {}", sessionId, e.getMessage()));
        
        knowledgeBaseChatService.chatWithKnowledge(sessionId, content, modelId)
                .subscribe(
                        chunk -> {
                            try {
                                if (chunk != null && !chunk.isEmpty()) {
                                    emitter.send(SseEmitter.event().data(chunk));
                                }
                            } catch (IOException e) {
                                log.error("发送SSE数据失败", e);
                                emitter.completeWithError(e);
                            }
                        },
                        error -> {
                            log.error("知识库对话错误", error);
                            try {
                                emitter.send(SseEmitter.event()
                                        .name("error")
                                        .data(Map.of("message", error.getMessage())));
                            } catch (IOException ignored) {}
                            emitter.completeWithError(error);
                        },
                        () -> {
                            try {
                                emitter.send(SseEmitter.event().name("done").data(""));
                            } catch (IOException ignored) {}
                            emitter.complete();
                        }
                );
        
        return emitter;
    }
    
    /**
     * 获取消息的引用来源
     */
    @GetMapping("/{messageId}/citations")
    public ResponseEntity<List<Map<String, Object>>> getCitations(@PathVariable String messageId) {
        List<Map<String, Object>> citations = knowledgeBaseChatService.getCitations(messageId);
        return ResponseEntity.ok(citations);
    }
}
