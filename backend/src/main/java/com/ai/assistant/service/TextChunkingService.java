package com.ai.assistant.service;

import com.ai.assistant.entity.DocumentChunk;
import com.ai.assistant.mapper.DocumentChunkMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class TextChunkingService {
    
    private final DocumentChunkMapper documentChunkMapper;
    
    @Value("${document.chunking.strategy:smart}")
    private String chunkingStrategy;
    
    // 段落分隔符模式：连续的换行符（2个或更多）
    private static final Pattern PARAGRAPH_PATTERN = Pattern.compile("\n{2,}");
    
    // 句子结束符（中英文）
    private static final Pattern SENTENCE_END_PATTERN = Pattern.compile("[。！？.!?]+[\\s\"'）】]*");
    
    /**
     * 智能文本分块 - 根据配置选择策略
     */
    public List<TextChunk> splitIntoChunks(String text, int chunkSize, int overlap) {
        if (text == null || text.isEmpty()) {
            return new ArrayList<>();
        }
        
        log.info("开始文本分块，策略: {}, 文本长度: {}, 目标块大小: {}, 重叠: {}", 
                chunkingStrategy, text.length(), chunkSize, overlap);
        
        List<TextChunk> chunks;
        if ("paragraph".equalsIgnoreCase(chunkingStrategy)) {
            chunks = splitByParagraphsOnly(text);
        } else if ("smart".equalsIgnoreCase(chunkingStrategy)) {
            chunks = splitByParagraphs(text, chunkSize, overlap);
        } else {
            chunks = splitByFixedSize(text, chunkSize, overlap);
        }
        
        log.info("文本分块完成，共 {} 块", chunks.size());
        return chunks;
    }
    
    /**
     * 纯段落切片 - 一个段落一块，不合并
     */
    private List<TextChunk> splitByParagraphsOnly(String text) {
        List<TextChunk> chunks = new ArrayList<>();
        
        // 按段落分割
        List<String> paragraphs = splitIntoParagraphs(text);
        log.info("识别到 {} 个段落，每个段落独立成块", paragraphs.size());
        
        // 每个段落独立成块
        for (int i = 0; i < paragraphs.size(); i++) {
            String content = paragraphs.get(i).trim();
            if (!content.isEmpty()) {
                chunks.add(new TextChunk(i, content));
                log.debug("段落 {} 长度: {} 字符", i + 1, content.length());
            }
        }
        
        return chunks;
    }
    
    /**
     * 智能段落切片 - 基于段落结构
     * 优先按段落切分，段落过大时按句子切分
     */
    private List<TextChunk> splitByParagraphs(String text, int chunkSize, int overlap) {
        List<TextChunk> chunks = new ArrayList<>();
        
        // 第一步：按段落分割
        List<String> paragraphs = splitIntoParagraphs(text);
        log.info("识别到 {} 个段落", paragraphs.size());
        
        // 第二步：智能组合段落成块
        List<String> smartChunks = createSmartChunks(paragraphs, chunkSize, overlap);
        
        // 第三步：创建TextChunk对象
        for (int i = 0; i < smartChunks.size(); i++) {
            String content = smartChunks.get(i).trim();
            if (!content.isEmpty()) {
                chunks.add(new TextChunk(i, content));
            }
        }
        
        return chunks;
    }
    
    /**
     * 固定大小切片 - 传统方式
     */
    private List<TextChunk> splitByFixedSize(String text, int chunkSize, int overlap) {
        List<TextChunk> chunks = new ArrayList<>();
        int textLength = text.length();
        int start = 0;
        int index = 0;
        
        while (start < textLength) {
            int end = Math.min(start + chunkSize, textLength);
            
            // 尝试在句子边界处分割
            if (end < textLength) {
                int searchStart = Math.max(start, end - 100);
                int lastPeriod = -1;
                int lastNewline = -1;
                
                for (int i = end - 1; i >= searchStart; i--) {
                    char c = text.charAt(i);
                    if (c == '.' || c == '。' || c == '!' || c == '！' || c == '?' || c == '？') {
                        lastPeriod = i;
                        break;
                    } else if (c == '\n') {
                        if (lastNewline == -1) {
                            lastNewline = i;
                        }
                    }
                }
                
                int boundary = Math.max(lastPeriod, lastNewline);
                if (boundary > start && boundary < end) {
                    end = boundary + 1;
                }
            }
            
            String content = text.substring(start, end).trim();
            if (!content.isEmpty()) {
                chunks.add(new TextChunk(index++, content));
            }
            
            int nextStart = end - overlap;
            if (nextStart <= start) {
                start = end;
            } else {
                start = nextStart;
            }
        }
        
        return chunks;
    }
    
    /**
     * 将文本按段落分割
     */
    private List<String> splitIntoParagraphs(String text) {
        List<String> paragraphs = new ArrayList<>();
        
        // 按连续换行符分割段落
        String[] parts = PARAGRAPH_PATTERN.split(text);
        
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                paragraphs.add(trimmed);
            }
        }
        
        // 如果没有明显的段落分隔，尝试按单个换行符分割
        if (paragraphs.size() <= 1 && text.contains("\n")) {
            paragraphs.clear();
            String[] lines = text.split("\n");
            for (String line : lines) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    paragraphs.add(trimmed);
                }
            }
        }
        
        // 如果还是只有一个段落，按句子分割
        if (paragraphs.size() <= 1) {
            paragraphs = splitIntoSentences(text);
        }
        
        return paragraphs;
    }
    
    /**
     * 将文本按句子分割
     */
    private List<String> splitIntoSentences(String text) {
        List<String> sentences = new ArrayList<>();
        Matcher matcher = SENTENCE_END_PATTERN.matcher(text);
        
        int lastEnd = 0;
        while (matcher.find()) {
            String sentence = text.substring(lastEnd, matcher.end()).trim();
            if (!sentence.isEmpty()) {
                sentences.add(sentence);
            }
            lastEnd = matcher.end();
        }
        
        // 添加最后一个句子（如果有）
        if (lastEnd < text.length()) {
            String lastSentence = text.substring(lastEnd).trim();
            if (!lastSentence.isEmpty()) {
                sentences.add(lastSentence);
            }
        }
        
        return sentences.isEmpty() ? List.of(text) : sentences;
    }
    
    /**
     * 智能组合段落成块
     * 策略：
     * 1. 小段落合并到一起，直到接近目标大小
     * 2. 大段落单独成块，如果超过目标大小则按句子切分
     * 3. 保持段落完整性，避免在段落中间切断
     */
    private List<String> createSmartChunks(List<String> paragraphs, int targetSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();
        
        for (int i = 0; i < paragraphs.size(); i++) {
            String paragraph = paragraphs.get(i);
            int paragraphLength = paragraph.length();
            
            // 情况1：段落本身就很大（超过目标大小的1.5倍）
            if (paragraphLength > targetSize * 1.5) {
                // 先保存当前累积的块
                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString());
                    currentChunk = new StringBuilder();
                }
                
                // 大段落按句子切分
                List<String> sentences = splitIntoSentences(paragraph);
                chunks.addAll(createSmartChunks(sentences, targetSize, overlap));
                continue;
            }
            
            // 情况2：添加这个段落会超过目标大小
            if (currentChunk.length() > 0 && currentChunk.length() + paragraphLength > targetSize) {
                // 保存当前块
                chunks.add(currentChunk.toString());
                
                // 创建新块，可能包含重叠内容
                if (overlap > 0 && currentChunk.length() > overlap) {
                    String overlapText = getLastNChars(currentChunk.toString(), overlap);
                    currentChunk = new StringBuilder(overlapText);
                    currentChunk.append("\n\n");
                } else {
                    currentChunk = new StringBuilder();
                }
            }
            
            // 情况3：添加段落到当前块
            if (currentChunk.length() > 0) {
                currentChunk.append("\n\n");
            }
            currentChunk.append(paragraph);
        }
        
        // 添加最后一个块
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString());
        }
        
        return chunks;
    }
    
    /**
     * 获取字符串的最后N个字符（尽量在句子边界处）
     */
    private String getLastNChars(String text, int n) {
        if (text.length() <= n) {
            return text;
        }
        
        String lastPart = text.substring(text.length() - n);
        
        // 尝试从句子边界开始
        Matcher matcher = SENTENCE_END_PATTERN.matcher(lastPart);
        if (matcher.find()) {
            return lastPart.substring(matcher.end());
        }
        
        // 尝试从段落边界开始
        int firstNewline = lastPart.indexOf("\n\n");
        if (firstNewline > 0) {
            return lastPart.substring(firstNewline + 2);
        }
        
        return lastPart;
    }
    
    /**
     * 保存文档块到数据库
     */
    @Transactional
    public List<DocumentChunk> saveChunks(String documentId, List<TextChunk> textChunks) {
        List<DocumentChunk> documentChunks = new ArrayList<>();
        long now = System.currentTimeMillis();
        
        for (TextChunk textChunk : textChunks) {
            DocumentChunk chunk = DocumentChunk.builder()
                    .documentId(documentId)
                    .content(textChunk.getContent())
                    .chunkIndex(textChunk.getIndex())
                    .vectorId("") // 将在向量化后更新
                    .createdAt(now)
                    .build();
            
            documentChunkMapper.insert(chunk);
            documentChunks.add(chunk);
        }
        
        log.info("保存文档块成功，文档ID: {}, 块数: {}", documentId, documentChunks.size());
        return documentChunks;
    }
    
    /**
     * 更新文档块的向量ID
     */
    public void updateVectorId(String chunkId, String vectorId) {
        DocumentChunk chunk = DocumentChunk.builder()
                .id(chunkId)
                .vectorId(vectorId)
                .build();
        documentChunkMapper.updateById(chunk);
    }
    
    /**
     * 文本块数据类
     */
    public static class TextChunk {
        private final int index;
        private final String content;
        
        public TextChunk(int index, String content) {
            this.index = index;
            this.content = content;
        }
        
        public int getIndex() {
            return index;
        }
        
        public String getContent() {
            return content;
        }
    }
}
