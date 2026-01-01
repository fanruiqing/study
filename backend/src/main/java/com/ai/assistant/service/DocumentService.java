package com.ai.assistant.service;

import com.ai.assistant.dto.DocumentDTO;
import com.ai.assistant.entity.Document;
import com.ai.assistant.mapper.DocumentMapper;
import com.ai.assistant.mapper.DocumentChunkMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {
    
    private final DocumentMapper documentMapper;
    private final DocumentChunkMapper documentChunkMapper;
    private final ObjectMapper objectMapper;
    
    private static final List<String> ALLOWED_TYPES = Arrays.asList("pdf", "docx", "txt", "md");
    private static final String UPLOAD_DIR = "uploads/documents";
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    
    /**
     * 上传文档
     */
    @Transactional
    public Document upload(String knowledgeBaseId, MultipartFile file, List<String> tags, String category) throws IOException {
        // 验证文件
        validateFile(file);
        
        // 保存文件
        String filePath = saveFile(file);
        
        // 创建文档记录
        Document document = Document.builder()
                .knowledgeBaseId(knowledgeBaseId)
                .fileName(file.getOriginalFilename())
                .fileType(getFileExtension(file.getOriginalFilename()))
                .fileSize(file.getSize())
                .filePath(filePath)
                .status("PENDING")
                .chunkCount(0)
                .tags(tags != null ? convertTagsToJson(tags) : null)
                .category(category)
                .createdAt(System.currentTimeMillis())
                .updatedAt(System.currentTimeMillis())
                .build();
        
        documentMapper.insert(document);
        log.info("文档上传成功: {}, ID: {}", document.getFileName(), document.getId());
        
        return document;
    }
    
    /**
     * 验证文件
     */
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }
        
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("文件大小不能超过 10MB");
        }
        
        String extension = getFileExtension(file.getOriginalFilename());
        if (!ALLOWED_TYPES.contains(extension.toLowerCase())) {
            throw new IllegalArgumentException("不支持的文件类型: " + extension + "。支持的类型: " + String.join(", ", ALLOWED_TYPES));
        }
    }
    
    /**
     * 保存文件到磁盘
     */
    private String saveFile(MultipartFile file) throws IOException {
        // 创建上传目录
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        // 生成唯一文件名
        String extension = getFileExtension(file.getOriginalFilename());
        String fileName = UUID.randomUUID().toString() + "." + extension;
        Path filePath = uploadPath.resolve(fileName);
        
        // 保存文件
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        return filePath.toString();
    }
    
    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }
    
    /**
     * 转换标签为 JSON
     */
    private String convertTagsToJson(List<String> tags) {
        try {
            return objectMapper.writeValueAsString(tags);
        } catch (Exception e) {
            log.error("转换标签为 JSON 失败", e);
            return "[]";
        }
    }
    
    /**
     * 删除文档
     */
    @Transactional
    public void delete(String documentId) {
        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new IllegalArgumentException("文档不存在: " + documentId);
        }
        
        // 删除文件
        try {
            Path filePath = Paths.get(document.getFilePath());
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.error("删除文件失败: {}", document.getFilePath(), e);
        }
        
        // 删除数据库记录（级联删除文档块）
        documentMapper.deleteById(documentId);
        log.info("文档删除成功: {}", documentId);
    }
    
    /**
     * 获取文档详情
     */
    public Document getById(String documentId) {
        return documentMapper.selectById(documentId);
    }
    
    /**
     * 获取知识库的所有文档
     */
    public List<Document> listByKnowledgeBase(String knowledgeBaseId) {
        LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Document::getKnowledgeBaseId, knowledgeBaseId)
                .orderByDesc(Document::getCreatedAt);
        return documentMapper.selectList(wrapper);
    }
    
    /**
     * 更新文档状态
     */
    public void updateStatus(String documentId, String status, String errorMessage) {
        Document document = Document.builder()
                .id(documentId)
                .status(status)
                .errorMessage(errorMessage)
                .updatedAt(System.currentTimeMillis())
                .build();
        documentMapper.updateById(document);
    }
    
    /**
     * 更新文档块数量
     */
    public void updateChunkCount(String documentId, int chunkCount) {
        Document document = Document.builder()
                .id(documentId)
                .chunkCount(chunkCount)
                .updatedAt(System.currentTimeMillis())
                .build();
        documentMapper.updateById(document);
    }
    
    /**
     * 转换为 DTO
     */
    public DocumentDTO toDTO(Document document) {
        List<String> tags = null;
        if (document.getTags() != null) {
            try {
                tags = objectMapper.readValue(document.getTags(), List.class);
            } catch (Exception e) {
                log.error("解析标签失败", e);
            }
        }
        
        return DocumentDTO.builder()
                .id(document.getId())
                .knowledgeBaseId(document.getKnowledgeBaseId())
                .fileName(document.getFileName())
                .fileType(document.getFileType())
                .fileSize(document.getFileSize())
                .status(document.getStatus())
                .errorMessage(document.getErrorMessage())
                .chunkCount(document.getChunkCount())
                .tags(tags)
                .category(document.getCategory())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }
}
