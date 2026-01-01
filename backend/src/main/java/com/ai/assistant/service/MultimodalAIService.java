package com.ai.assistant.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 自定义多模态 AI 服务
 * 直接调用 OpenAI API，支持真正的图片识别和多模态
 */
@Slf4j
@Service
public class MultimodalAIService {
    
    private final OkHttpClient client;
    private final Gson gson;
    
    public MultimodalAIService() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(300, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
    }
    
    /**
     * 流式调用 OpenAI API（支持多模态）
     */
    public Flux<String> streamChat(
            String apiKey,
            String baseUrl,
            String model,
            List<Map<String, Object>> messages,
            Double temperature,
            Integer maxTokens
    ) {
        return Flux.<String>create(sink -> {
            try {
                // 构建请求体
                JsonObject requestBody = new JsonObject();
                requestBody.addProperty("model", model);
                requestBody.addProperty("stream", true);
                if (temperature != null) {
                    requestBody.addProperty("temperature", temperature);
                }
                if (maxTokens != null) {
                    requestBody.addProperty("max_tokens", maxTokens);
                }
                
                // 添加消息
                JsonArray messagesArray = new JsonArray();
                for (Map<String, Object> message : messages) {
                    JsonObject messageObj = new JsonObject();
                    messageObj.addProperty("role", (String) message.get("role"));
                    
                    Object content = message.get("content");
                    if (content instanceof String) {
                        // 纯文本消息
                        messageObj.addProperty("content", (String) content);
                    } else if (content instanceof List) {
                        // 多模态消息（文本 + 图片）
                        JsonArray contentArray = new JsonArray();
                        for (Object item : (List<?>) content) {
                            if (item instanceof Map) {
                                Map<?, ?> contentItem = (Map<?, ?>) item;
                                JsonObject contentObj = new JsonObject();
                                contentObj.addProperty("type", (String) contentItem.get("type"));
                                
                                if ("text".equals(contentItem.get("type"))) {
                                    contentObj.addProperty("text", (String) contentItem.get("text"));
                                } else if ("image_url".equals(contentItem.get("type"))) {
                                    // 处理 image_url 对象
                                    Object imageUrlObj = contentItem.get("image_url");
                                    if (imageUrlObj instanceof Map) {
                                        JsonObject imageUrl = new JsonObject();
                                        imageUrl.addProperty("url", (String) ((Map<?, ?>) imageUrlObj).get("url"));
                                        contentObj.add("image_url", imageUrl);
                                    } else if (imageUrlObj instanceof String) {
                                        // 兼容旧格式
                                        JsonObject imageUrl = new JsonObject();
                                        imageUrl.addProperty("url", (String) imageUrlObj);
                                        contentObj.add("image_url", imageUrl);
                                    }
                                }
                                
                                contentArray.add(contentObj);
                            }
                        }
                        messageObj.add("content", contentArray);
                    }
                    
                    messagesArray.add(messageObj);
                }
                requestBody.add("messages", messagesArray);
                
                log.info("发送请求到 OpenAI API: {}", model);
                log.debug("请求体: {}", requestBody);
                
                // 构建请求
                String url = baseUrl.endsWith("/") ? baseUrl + "v1/chat/completions" : baseUrl + "/v1/chat/completions";
                Request request = new Request.Builder()
                        .url(url)
                        .addHeader("Authorization", "Bearer " + apiKey)
                        .addHeader("Content-Type", "application/json")
                        .post(RequestBody.create(
                                requestBody.toString(),
                                MediaType.parse("application/json")
                        ))
                        .build();
                
                // 发送请求
                Response response = null;
                try {
                    response = client.newCall(request).execute();
                    
                    if (!response.isSuccessful()) {
                        String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                        log.error("OpenAI API 错误: {} - {}", response.code(), errorBody);
                        sink.error(new IOException("OpenAI API error: " + response.code() + " - " + errorBody));
                        if (response != null) response.close();
                        return;
                    }
                    
                    // 读取流式响应
                    ResponseBody body = response.body();
                    if (body == null) {
                        sink.error(new IOException("Response body is null"));
                        if (response != null) response.close();
                        return;
                    }
                    
                    // 使用 BufferedReader 直接从 InputStream 读取，而不是先转成 String
                    BufferedReader reader = new BufferedReader(body.charStream());
                    String line;
                    
                    final Response finalResponse = response;
                    sink.onDispose(() -> {
                        try {
                            reader.close();
                            if (finalResponse != null) {
                                finalResponse.close();
                            }
                        } catch (IOException e) {
                            log.warn("关闭响应失败", e);
                        }
                    });
                    
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("data: ")) {
                            String data = line.substring(6);
                            
                            if ("[DONE]".equals(data)) {
                                log.info("✓ 流式响应完成");
                                sink.complete();
                                reader.close();
                                finalResponse.close();
                                break;
                            }
                            
                            try {
                                JsonObject json = gson.fromJson(data, JsonObject.class);
                                JsonArray choices = json.getAsJsonArray("choices");
                                if (choices != null && choices.size() > 0) {
                                    JsonObject choice = choices.get(0).getAsJsonObject();
                                    JsonObject delta = choice.getAsJsonObject("delta");
                                    if (delta != null) {
                                        // 检查是否有思考内容 (DeepSeek reasoning_content)
                                        if (delta.has("reasoning_content") && !delta.get("reasoning_content").isJsonNull()) {
                                            String reasoningContent = delta.get("reasoning_content").getAsString();
                                            if (reasoningContent != null && !reasoningContent.isEmpty()) {
                                                // 发送思考内容，使用特殊前缀标记
                                                sink.next("[[THINKING]]" + reasoningContent);
                                            }
                                        }
                                        // 检查正常内容
                                        if (delta.has("content") && !delta.get("content").isJsonNull()) {
                                            String content = delta.get("content").getAsString();
                                            if (content != null && !content.isEmpty()) {
                                                sink.next(content);
                                            }
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                log.warn("解析响应失败: {}", data, e);
                            }
                        }
                    }
                    
                } catch (IOException e) {
                    log.error("请求失败", e);
                    sink.error(e);
                    if (response != null) {
                        response.close();
                    }
                }
                
            } catch (Exception e) {
                log.error("构建请求失败", e);
                sink.error(e);
            }
        }).subscribeOn(Schedulers.boundedElastic());  // 在单独的线程上运行，确保流式响应能立即传递
    }
    
    /**
     * 构建多模态消息内容
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> buildMultimodalContent(String text, List<String> imageUrls) {
        List<Map<String, Object>> content = new ArrayList<>();
        
        // 添加文本
        content.add(Map.of("type", "text", "text", text));
        
        // 添加图片
        for (String url : imageUrls) {
            content.add(Map.of("type", "image_url", "url", url));
        }
        
        return content;
    }
}
