package com.ai.assistant.service;

import com.ai.assistant.dto.MessageDTO;
import com.ai.assistant.dto.RetrievedChunk;
import com.ai.assistant.entity.KnowledgeBase;
import com.ai.assistant.entity.Message;
import com.ai.assistant.entity.ModelProvider;
import com.ai.assistant.mapper.MessageMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 带知识库的聊天服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseChatService {
    
    private final ChatService chatService;
    private final EnhancedChatService enhancedChatService;
    private final RAGService ragService;
    private final KnowledgeBaseService knowledgeBaseService;
    private final MessageMapper messageMapper;
    private final ObjectMapper objectMapper;
    private final MultimodalAIService multimodalAIService;
    
    // 存储消息ID到引用信息的映射（临时存储，用于流式响应完成后保存）
    private final Map<String, List<RetrievedChunk>> pendingCitations = new HashMap<>();
    
    /**
     * 带知识库的流式对话
     */
    public Flux<String> chatWithKnowledge(String sessionId, String userMessage, String modelId) {
        log.info("开始带知识库的对话，会话: {}, 消息: {}", sessionId, userMessage);
        
        // 获取会话关联的知识库
        List<KnowledgeBase> knowledgeBases = knowledgeBaseService.getBySessionId(sessionId);
        
        if (knowledgeBases.isEmpty()) {
            log.info("会话未关联知识库，使用普通对话");
            return enhancedChatService.streamChat(sessionId, userMessage, modelId, null, null);
        }
        
        // 提取知识库ID
        List<String> kbIds = knowledgeBases.stream()
                .map(KnowledgeBase::getId)
                .collect(Collectors.toList());
        
        log.info("会话关联了 {} 个知识库", kbIds.size());
        
        // 使用第一个知识库的embedding模型
        String embeddingModel = knowledgeBases.get(0).getEmbeddingModel();
        
        // 检索相关知识
        List<RetrievedChunk> chunks = ragService.retrieve(userMessage, kbIds, 3, embeddingModel);
        
        if (chunks.isEmpty()) {
            log.info("未检索到相关知识，使用普通对话");
            return enhancedChatService.streamChat(sessionId, userMessage, modelId, null, null);
        }
        
        log.info("检索到 {} 个相关文档块", chunks.size());
        
        // 先保存用户原始消息
        Message userMsg = new Message();
        userMsg.setId(UUID.randomUUID().toString());
        userMsg.setSessionId(sessionId);
        userMsg.setRole("USER");
        userMsg.setContent(userMessage);  // 保存原始用户消息，不是增强后的 prompt
        userMsg.setTimestamp(System.currentTimeMillis());
        userMsg.setModelId(modelId);
        messageMapper.insert(userMsg);
        log.info("✓ 用户原始消息已保存");
        
        // 构建带上下文和引用标记的提示词（仅用于发送给 AI）
        String enhancedPrompt = buildPromptWithInlineCitations(userMessage, chunks);
        
        // 生成临时消息ID用于存储引用信息
        String tempMessageId = UUID.randomUUID().toString();
        pendingCitations.put(tempMessageId, chunks);
        
        // 使用增强的提示词进行对话，但不再保存用户消息（已经保存过了）
        return streamChatWithoutSavingUserMessage(sessionId, enhancedPrompt, modelId, chunks);
    }
    
    /**
     * 流式对话（不保存用户消息，因为已经单独保存了原始消息）
     * 使用 MultimodalAIService 以支持思考过程显示
     */
    private Flux<String> streamChatWithoutSavingUserMessage(String sessionId, String enhancedPrompt, String modelId, List<RetrievedChunk> chunks) {
        // 解析 modelId
        final String providerId;
        final String modelName;
        if (modelId != null && modelId.contains(":")) {
            String[] parts = modelId.split(":", 2);
            providerId = parts[0];
            modelName = parts[1];
        } else {
            providerId = modelId;
            modelName = null;
        }
        
        // 获取历史消息（包括刚保存的用户消息）
        List<Message> history = messageMapper.findBySessionIdOrderByTimestampAsc(sessionId);
        
        // 构建消息列表（使用 MultimodalAIService 的格式）
        List<Map<String, Object>> messages = new ArrayList<>();
        for (int i = 0; i < history.size(); i++) {
            Message msg = history.get(i);
            Map<String, Object> messageMap = new HashMap<>();
            messageMap.put("role", msg.getRole().toLowerCase());
            
            if ("USER".equals(msg.getRole())) {
                // 如果是最后一条用户消息，使用增强后的 prompt
                if (i == history.size() - 1) {
                    messageMap.put("content", enhancedPrompt);
                } else {
                    messageMap.put("content", msg.getContent());
                }
            } else if ("ASSISTANT".equals(msg.getRole())) {
                messageMap.put("content", msg.getContent());
            }
            
            messages.add(messageMap);
        }
        
        // 获取模型提供商
        var provider = knowledgeBaseService.getModelProvider(providerId);
        if (provider == null) {
            return Flux.error(new RuntimeException("未找到模型提供商: " + providerId));
        }
        
        final String finalModelName = modelName != null ? modelName : provider.getModelName();
        final String finalModelId = modelId;
        
        String assistantMessageId = UUID.randomUUID().toString();
        StringBuilder responseBuilder = new StringBuilder();      // 正式回答内容
        StringBuilder thinkingBuilder = new StringBuilder();      // 思考过程内容
        
        // 使用 MultimodalAIService 以支持思考过程
        log.info("开始流式调用 AI 模型（支持思考过程）...");
        return multimodalAIService.streamChat(
                provider.getApiKey(),
                provider.getBaseUrl() != null ? provider.getBaseUrl() : "https://api.openai.com",
                finalModelName,
                messages,
                provider.getTemperature() != null ? provider.getTemperature() : 0.7,
                provider.getMaxTokens() != null ? provider.getMaxTokens() : 2048
        )
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
        .doOnComplete(() -> {
            log.info("✓ AI 响应完成，回答长度: {} 字符，思考长度: {} 字符", 
                    responseBuilder.length(), thinkingBuilder.length());
            // 保存助手消息
            Message assistantMessage = new Message();
            assistantMessage.setId(assistantMessageId);
            assistantMessage.setSessionId(sessionId);
            assistantMessage.setRole("ASSISTANT");
            assistantMessage.setContent(responseBuilder.toString());
            assistantMessage.setTimestamp(System.currentTimeMillis());
            assistantMessage.setModelId(finalModelId);
            
            // 保存引用信息和思考内容到 metadata
            try {
                List<Map<String, Object>> citationsList = chunks.stream()
                    .map(chunk -> {
                        Map<String, Object> citation = new HashMap<>();
                        citation.put("documentId", chunk.getDocumentId());
                        citation.put("documentName", chunk.getDocumentName());
                        citation.put("chunkId", chunk.getChunkId());
                        citation.put("content", chunk.getContent());
                        citation.put("similarity", chunk.getRelevanceScore());
                        return citation;
                    })
                    .collect(Collectors.toList());
                
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("citations", citationsList);
                
                // 如果有思考内容，也保存到 metadata
                if (thinkingBuilder.length() > 0) {
                    metadata.put("thinking", thinkingBuilder.toString());
                }
                
                assistantMessage.setMetadata(objectMapper.writeValueAsString(metadata));
            } catch (Exception e) {
                log.error("序列化 metadata 失败", e);
            }
            
            messageMapper.insert(assistantMessage);
            log.info("✓ 助手消息已保存，包含 {} 个引用", chunks.size());
        });
    }
    
    /**
     * 构建带内联引用标记的提示词
     */
    private String buildPromptWithInlineCitations(String userMessage, List<RetrievedChunk> chunks) {
        StringBuilder prompt = new StringBuilder();
        
        // 系统指令：告诉 AI 如何使用引用
        prompt.append("<system_instruction>\n");
        prompt.append("你是一个专业的知识库助手。在回答问题时，你需要：\n");
        prompt.append("1. 基于提供的知识库内容回答用户问题\n");
        prompt.append("2. 当引用知识库内容时，在相关句子或段落后添加引用标记：[^1], [^2], [^3] 等\n");
        prompt.append("3. 引用标记应该紧跟在引用内容之后，不要单独成行\n");
        prompt.append("4. 可以在一句话中使用多个引用，例如：这个功能很强大[^1]，并且易于使用[^2]。\n");
        prompt.append("5. 如果知识库中没有相关信息，请明确告知用户\n");
        prompt.append("6. 用自然、流畅的语言回答，不要直接复制粘贴知识库内容\n");
        prompt.append("</system_instruction>\n\n");
        
        // 知识库内容（简洁版）
        prompt.append("<knowledge_base>\n");
        for (int i = 0; i < chunks.size(); i++) {
            RetrievedChunk chunk = chunks.get(i);
            prompt.append(String.format("[文档%d: %s]\n", i + 1, chunk.getDocumentName()));
            prompt.append(chunk.getContent().trim());
            prompt.append("\n\n");
        }
        prompt.append("</knowledge_base>\n\n");
        
        // 用户问题
        prompt.append("<user_question>\n");
        prompt.append(userMessage);
        prompt.append("\n</user_question>\n\n");
        
        prompt.append("请基于知识库内容回答用户问题，并在引用时使用 [^1], [^2] 等标记：\n");
        
        return prompt.toString();
    }
    
    /**
     * 获取消息的引用来源
     */
    public List<Map<String, Object>> getCitations(String messageId) {
        Message message = messageMapper.selectById(messageId);
        
        if (message == null || message.getMetadata() == null || message.getMetadata().isEmpty()) {
            return List.of();
        }
        
        try {
            Map<String, Object> metadata = objectMapper.readValue(
                message.getMetadata(), 
                new TypeReference<Map<String, Object>>() {}
            );
            
            Object citations = metadata.get("citations");
            if (citations instanceof List) {
                return (List<Map<String, Object>>) citations;
            }
        } catch (Exception e) {
            log.error("解析引用信息失败", e);
        }
        
        return List.of();
    }
}
