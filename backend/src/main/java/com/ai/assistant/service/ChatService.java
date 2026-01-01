package com.ai.assistant.service;

import com.ai.assistant.dto.MessageDTO;
import com.ai.assistant.entity.Message;
import com.ai.assistant.entity.ModelProvider;
import com.ai.assistant.mapper.MessageMapper;
import com.ai.assistant.mapper.ModelProviderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {
    
    private final MessageMapper messageMapper;
    private final ModelProviderMapper modelProviderMapper;
    private final SessionService sessionService;
    private final ModelProviderService modelProviderService;
    
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
    
    public Flux<String> streamChat(String sessionId, String content, String modelId, List<MessageDTO.AttachmentDTO> attachments) {
        log.info("=== ChatService.streamChat 开始 ===");
        
        // 解析 modelId，支持 providerId:modelName 格式
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
        
        // Save user message
        log.info("保存用户消息...");
        Message userMessage = new Message();
        userMessage.setId(UUID.randomUUID().toString());
        userMessage.setSessionId(sessionId);
        userMessage.setRole("USER");
        userMessage.setContent(content);
        userMessage.setTimestamp(System.currentTimeMillis());
        userMessage.setModelId(modelId);
        
        // 保存附件信息到 metadata
        if (attachments != null && !attachments.isEmpty()) {
            MessageDTO tempDto = new MessageDTO();
            tempDto.setAttachments(attachments);
            Message tempMessage = tempDto.toEntity();
            userMessage.setMetadata(tempMessage.getMetadata());
            log.info("✓ 保存了 {} 个附件", attachments.size());
        }
        
        messageMapper.insert(userMessage);
        log.info("✓ 用户消息已保存");
        
        // Get chat history
        List<Message> history = messageMapper.findBySessionIdOrderByTimestampAsc(sessionId);
        log.info("加载历史消息: {} 条", history.size());
        
        // Get model provider
        log.info("获取模型提供商: {}", providerId);
        ModelProvider provider = modelProviderMapper.selectById(providerId);
        if (provider == null) {
            log.warn("未找到指定提供商，使用默认提供商");
            provider = getDefaultProvider();
        }
        log.info("✓ 使用提供商: {} ({})", provider.getName(), provider.getBaseUrl());
        
        // 如果指定了模型名称，使用指定的；否则使用提供商默认的
        final String finalModelName = modelName != null ? modelName : provider.getModelName();
        final String finalModelId = modelId;
        log.info("✓ 使用模型: {}", finalModelName);
        
        // Create chat client
        log.info("创建聊天客户端...");
        OpenAiChatModel chatModel = createChatModel(provider, finalModelName);
        log.info("✓ 聊天客户端已创建");
        
        // Build messages
        // 注意：当前 Spring AI 版本不支持多模态，我们在消息中添加附件说明
        List<org.springframework.ai.chat.messages.Message> messages = new ArrayList<>();
        for (Message msg : history) {
            if ("USER".equals(msg.getRole())) {
                String messageContent = msg.getContent();
                
                // 检查是否有附件
                MessageDTO msgDto = MessageDTO.fromEntity(msg);
                if (msgDto.getAttachments() != null && !msgDto.getAttachments().isEmpty()) {
                    // 添加附件说明（等待 Spring AI 支持真正的多模态）
                    StringBuilder contentBuilder = new StringBuilder(messageContent);
                    contentBuilder.append("\n\n[用户上传了以下附件]:\n");
                    for (MessageDTO.AttachmentDTO attachment : msgDto.getAttachments()) {
                        contentBuilder.append("- ").append(attachment.getType())
                                     .append(": ").append(attachment.getName())
                                     .append(" (").append(attachment.getMimeType()).append(")\n");
                    }
                    contentBuilder.append("\n注意：当前版本暂不支持直接查看图片内容，请根据文件名和类型提供建议。");
                    messageContent = contentBuilder.toString();
                    log.info("添加了附件说明到消息中");
                }
                
                messages.add(new UserMessage(messageContent));
            } else if ("ASSISTANT".equals(msg.getRole())) {
                messages.add(new AssistantMessage(msg.getContent()));
            }
        }
        log.info("构建消息列表: {} 条", messages.size());
        
        Prompt prompt = new Prompt(messages);
        
        // Create assistant message placeholder
        String assistantMessageId = UUID.randomUUID().toString();
        StringBuilder responseBuilder = new StringBuilder();
        
        log.info("开始流式调用 AI 模型...");
        return chatModel.stream(prompt)
                .doOnSubscribe(s -> log.info("→ 已订阅流"))
                .map(response -> {
                    String chunk = response.getResult().getOutput().getContent();
                    if (chunk != null) {
                        responseBuilder.append(chunk);
                    }
                    return chunk != null ? chunk : "";
                })
                .doOnNext(chunk -> {
                    if (!chunk.isEmpty()) {
                        log.debug("← 收到响应块: {} 字符", chunk.length());
                    }
                })
                .doOnComplete(() -> {
                    log.info("✓ AI 响应完成，总长度: {} 字符", responseBuilder.length());
                    // Save assistant message
                    Message assistantMessage = new Message();
                    assistantMessage.setId(assistantMessageId);
                    assistantMessage.setSessionId(sessionId);
                    assistantMessage.setRole("ASSISTANT");
                    assistantMessage.setContent(responseBuilder.toString());
                    assistantMessage.setTimestamp(System.currentTimeMillis());
                    assistantMessage.setModelId(finalModelId);
                    messageMapper.insert(assistantMessage);
                    sessionService.updateSessionTimestamp(sessionId);
                    log.info("✓ 助手消息已保存");
                })
                .doOnError(error -> {
                    log.error("✗ 流式调用错误: {}", error.getMessage(), error);
                    // 标记模型为失败
                    modelProviderService.markModelAsFailed(providerId, finalModelName);
                });
    }
    
    // 兼容旧接口
    public Flux<String> streamChat(String sessionId, String content, String modelId) {
        return streamChat(sessionId, content, modelId, null);
    }
    
    public Flux<String> regenerateMessage(String messageId) {
        Message message = messageMapper.selectById(messageId);
        if (message == null) {
            throw new RuntimeException("Message not found: " + messageId);
        }
        
        if (!"ASSISTANT".equals(message.getRole())) {
            throw new RuntimeException("Can only regenerate assistant messages");
        }
        
        // Find the previous user message
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
        
        // Delete the current assistant message
        messageMapper.deleteById(messageId);
        
        // Regenerate
        MessageDTO userDto = MessageDTO.fromEntity(previousUserMessage);
        return streamChat(message.getSessionId(), previousUserMessage.getContent(), 
                         message.getModelId(), userDto.getAttachments());
    }
    
    public void rateMessage(String messageId, int rating) {
        Message message = messageMapper.selectById(messageId);
        if (message == null) {
            throw new RuntimeException("Message not found: " + messageId);
        }
        message.setRating(rating);
        messageMapper.updateById(message);
    }
    
    private OpenAiChatModel createChatModel(ModelProvider provider, String modelName) {
        String apiKey = provider.getApiKey();
        String baseUrl = provider.getBaseUrl() != null ? provider.getBaseUrl() : "https://api.openai.com";
        
        OpenAiApi api = new OpenAiApi(baseUrl, apiKey);
        
        String model = modelName != null ? modelName : 
                       (provider.getModelName() != null ? provider.getModelName() : "gpt-3.5-turbo");
        
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .withModel(model)
                .withTemperature(provider.getTemperature() != null ? provider.getTemperature() : 0.7)
                .withMaxTokens(provider.getMaxTokens() != null ? provider.getMaxTokens() : 2048)
                .build();
        
        return new OpenAiChatModel(api, options);
    }
    
    private OpenAiChatModel createChatModel(ModelProvider provider) {
        return createChatModel(provider, null);
    }
    
    private ModelProvider getDefaultProvider() {
        List<ModelProvider> activeProviders = modelProviderMapper.findByIsActiveTrue();
        if (activeProviders.isEmpty()) {
            throw new RuntimeException("No active model provider found");
        }
        return activeProviders.get(0);
    }
}
