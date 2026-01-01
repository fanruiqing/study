package com.ai.assistant.service;

import com.ai.assistant.dto.MessageDTO;
import com.ai.assistant.entity.Message;
import com.ai.assistant.entity.ModelProvider;
import com.ai.assistant.mapper.MessageMapper;
import com.ai.assistant.mapper.ModelProviderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 增强版聊天服务
 * 支持真正的多模态（图片识别）和文档内容提取
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnhancedChatService {
    
    private final MessageMapper messageMapper;
    private final ModelProviderMapper modelProviderMapper;
    private final SessionService sessionService;
    private final ModelProviderService modelProviderService;
    private final MultimodalAIService multimodalAIService;
    private final DocumentExtractorService documentExtractorService;
    
    public List<MessageDTO> getMessages(String sessionId) {
        return messageMapper.findBySessionIdOrderByTimestampAsc(sessionId)
                .stream()
                .map(MessageDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    public MessageDTO saveMessage(MessageDTO dto) {
        Message message = dto.toEntity();
        if (message.getId() == null) {
            message.setId(UUID.randomUUID().toString());
        }
        if (message.getTimestamp() == null) {
            message.setTimestamp(System.currentTimeMillis());
        }
        messageMapper.insert(message);
        sessionService.updateSessionTimestamp(message.getSessionId());
        return MessageDTO.fromEntity(message);
    }
    
    @Transactional
    public void deleteMessage(String messageId) {
        Message message = messageMapper.selectById(messageId);
        if (message == null) {
            throw new RuntimeException("Message not found: " + messageId);
        }
        
        messageMapper.deleteBySessionIdAndTimestampGreaterThanEqual(
                message.getSessionId(), message.getTimestamp());
    }
    
    /**
     * 流式聊天（支持真正的多模态）
     */
    public Flux<String> streamChat(
            String sessionId, 
            String content, 
            String modelId, 
            List<MessageDTO.AttachmentDTO> attachments,
            String extractedContent
    ) {
        log.info("=== EnhancedChatService.streamChat 开始 ===");
        
        // 解析 modelId
        final String providerId;
        final String modelName;
        if (modelId != null && modelId.contains(":")) {
            String[] parts = modelId.split(":", 2);
            providerId = parts[0];
            modelName = parts[1];
            log.info("解析模型ID - Provider: {}, Model: {}", providerId, modelName);
        } else {
            providerId = modelId;
            modelName = null;
            log.info("使用完整模型ID: {}", modelId);
        }
        
        // 保存用户消息
        log.info("保存用户消息...");
        Message userMessage = new Message();
        userMessage.setId(UUID.randomUUID().toString());
        userMessage.setSessionId(sessionId);
        userMessage.setRole("USER");
        
        // 如果有提取的文档内容，添加到消息中
        String finalContent = content;
        if (extractedContent != null && !extractedContent.isEmpty()) {
            finalContent = content + "\n\n[文档内容]:\n" + extractedContent;
            log.info("✓ 添加了提取的文档内容，长度: {} 字符", extractedContent.length());
        }
        userMessage.setContent(finalContent);
        
        userMessage.setTimestamp(System.currentTimeMillis());
        userMessage.setModelId(modelId);
        
        // 保存附件信息
        if (attachments != null && !attachments.isEmpty()) {
            MessageDTO tempDto = new MessageDTO();
            tempDto.setAttachments(attachments);
            Message tempMessage = tempDto.toEntity();
            userMessage.setMetadata(tempMessage.getMetadata());
            log.info("✓ 保存了 {} 个附件", attachments.size());
        }
        
        messageMapper.insert(userMessage);
        log.info("✓ 用户消息已保存");
        
        // 获取历史消息
        List<Message> history = messageMapper.findBySessionIdOrderByTimestampAsc(sessionId);
        log.info("加载历史消息: {} 条", history.size());
        
        // 获取模型提供商
        log.info("获取模型提供商: {}", providerId);
        ModelProvider provider = modelProviderMapper.selectById(providerId);
        if (provider == null) {
            log.warn("未找到指定提供商，使用默认提供商");
            provider = getDefaultProvider();
        }
        log.info("✓ 使用提供商: {} ({})", provider.getName(), provider.getBaseUrl());
        
        final String finalModelName = modelName != null ? modelName : provider.getModelName();
        final String finalModelId = modelId;
        log.info("✓ 使用模型: {}", finalModelName);
        
        // 构建消息列表（支持多模态）
        List<Map<String, Object>> messages = new ArrayList<>();
        for (Message msg : history) {
            Map<String, Object> messageMap = new HashMap<>();
            messageMap.put("role", msg.getRole().toLowerCase());
            
            if ("USER".equals(msg.getRole())) {
                // 检查是否有图片附件
                MessageDTO msgDto = MessageDTO.fromEntity(msg);
                if (msgDto.getAttachments() != null && !msgDto.getAttachments().isEmpty()) {
                    // 检查是否有图片
                    List<String> imageUrls = msgDto.getAttachments().stream()
                            .filter(att -> "image".equals(att.getType()))
                            .map(MessageDTO.AttachmentDTO::getUrl)
                            .collect(Collectors.toList());
                    
                    if (!imageUrls.isEmpty()) {
                        // 多模态消息：文本 + 图片
                        List<Map<String, Object>> contentList = new ArrayList<>();
                        
                        // 添加文本
                        contentList.add(Map.of("type", "text", "text", msg.getContent()));
                        
                        // 添加图片
                        for (String imageUrl : imageUrls) {
                            Map<String, Object> imageContent = new HashMap<>();
                            imageContent.put("type", "image_url");
                            Map<String, String> imageUrlMap = new HashMap<>();
                            imageUrlMap.put("url", imageUrl);
                            imageContent.put("image_url", imageUrlMap);
                            contentList.add(imageContent);
                        }
                        
                        messageMap.put("content", contentList);
                        log.info("✓ 添加多模态消息: 文本 + {} 个图片", imageUrls.size());
                    } else {
                        // 纯文本消息
                        messageMap.put("content", msg.getContent());
                    }
                } else {
                    // 纯文本消息
                    messageMap.put("content", msg.getContent());
                }
            } else {
                // 助手消息
                messageMap.put("content", msg.getContent());
            }
            
            messages.add(messageMap);
        }
        log.info("构建消息列表: {} 条", messages.size());
        
        // 创建助手消息占位符
        String assistantMessageId = UUID.randomUUID().toString();
        StringBuilder responseBuilder = new StringBuilder();      // 正式回答内容
        StringBuilder thinkingBuilder = new StringBuilder();      // 思考过程内容
        
        // 使用自定义多模态服务
        log.info("开始流式调用 AI 模型（多模态）...");
        return multimodalAIService.streamChat(
                provider.getApiKey(),
                provider.getBaseUrl(),
                finalModelName,
                messages,
                provider.getTemperature(),
                provider.getMaxTokens()
        )
        .doOnSubscribe(s -> log.info("→ 已订阅流"))
        .map(chunk -> {
            // 区分思考内容和正式回答
            if (chunk.startsWith("[[THINKING]]")) {
                // 思考内容，单独保存
                String thinkingChunk = chunk.substring(12);
                thinkingBuilder.append(thinkingChunk);
                log.debug("← 收到思考内容: {} 字符", thinkingChunk.length());
            } else {
                // 正式回答内容
                responseBuilder.append(chunk);
            }
            return chunk;
        })
        .doOnNext(chunk -> {
            if (!chunk.isEmpty() && !chunk.startsWith("[[THINKING]]")) {
                log.debug("← 收到响应块: {} 字符", chunk.length());
            }
        })
        .doOnComplete(() -> {
            log.info("✓ AI 响应完成，回答长度: {} 字符，思考长度: {} 字符", 
                    responseBuilder.length(), thinkingBuilder.length());
            // 保存助手消息（只保存正式回答，不包含思考内容）
            Message assistantMessage = new Message();
            assistantMessage.setId(assistantMessageId);
            assistantMessage.setSessionId(sessionId);
            assistantMessage.setRole("ASSISTANT");
            assistantMessage.setContent(responseBuilder.toString());
            assistantMessage.setTimestamp(System.currentTimeMillis());
            assistantMessage.setModelId(finalModelId);
            // 如果有思考内容，保存到 metadata
            if (thinkingBuilder.length() > 0) {
                assistantMessage.setMetadata("{\"thinking\":\"" + 
                    thinkingBuilder.toString()
                        .replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                        .replace("\n", "\\n")
                        .replace("\r", "\\r")
                        .replace("\t", "\\t") + "\"}");
            }
            messageMapper.insert(assistantMessage);
            sessionService.updateSessionTimestamp(sessionId);
            log.info("✓ 助手消息已保存");
        })
        .doOnError(error -> {
            log.error("✗ 流式调用错误: {}", error.getMessage(), error);
            modelProviderService.markModelAsFailed(providerId, finalModelName);
        });
    }
    
    // 兼容旧接口
    public Flux<String> streamChat(String sessionId, String content, String modelId) {
        return streamChat(sessionId, content, modelId, null, null);
    }
    
    public Flux<String> regenerateMessage(String messageId) {
        Message message = messageMapper.selectById(messageId);
        if (message == null) {
            throw new RuntimeException("Message not found: " + messageId);
        }
        
        if (!"ASSISTANT".equals(message.getRole())) {
            throw new RuntimeException("Can only regenerate assistant messages");
        }
        
        // 查找上一条用户消息
        List<Message> history = messageMapper.findBySessionIdOrderByTimestampAsc(message.getSessionId());
        Message previousUserMessage = null;
        for (int i = history.size() - 1; i >= 0; i--) {
            Message msg = history.get(i);
            if (msg.getTimestamp() < message.getTimestamp() && "USER".equals(msg.getRole())) {
                previousUserMessage = msg;
                break;
            }
        }
        
        if (previousUserMessage == null) {
            throw new RuntimeException("No previous user message found");
        }
        
        // 删除当前助手消息
        messageMapper.deleteById(messageId);
        
        // 重新生成
        MessageDTO userDto = MessageDTO.fromEntity(previousUserMessage);
        return streamChat(message.getSessionId(), previousUserMessage.getContent(), 
                         message.getModelId(), userDto.getAttachments(), null);
    }
    
    public void rateMessage(String messageId, int rating) {
        Message message = messageMapper.selectById(messageId);
        if (message == null) {
            throw new RuntimeException("Message not found: " + messageId);
        }
        message.setRating(rating);
        messageMapper.updateById(message);
    }
    
    private ModelProvider getDefaultProvider() {
        List<ModelProvider> activeProviders = modelProviderMapper.findByIsActiveTrue();
        if (activeProviders.isEmpty()) {
            throw new RuntimeException("No active model provider found");
        }
        return activeProviders.get(0);
    }
}
