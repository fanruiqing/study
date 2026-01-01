package com.ai.assistant.service;

import com.ai.assistant.config.FileUploadConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {
    
    private final FileUploadConfig fileUploadConfig;
    
    /**
     * 上传文件
     */
    public String uploadFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }
        
        // 检查文件大小
        long maxSize = fileUploadConfig.getMaxSize() * 1024 * 1024; // 转换为字节
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("文件大小不能超过 " + fileUploadConfig.getMaxSize() + "MB");
        }
        
        // 获取原始文件名和扩展名
        String originalFilename = file.getOriginalFilename();
        String extension = StringUtils.getFilenameExtension(originalFilename);
        
        // 验证文件类型
        if (!isAllowedFileType(extension)) {
            throw new IllegalArgumentException("不支持的文件类型: " + extension);
        }
        
        // 生成新文件名
        String newFilename = UUID.randomUUID().toString() + "." + extension;
        
        // 创建上传目录
        Path uploadPath = Paths.get(fileUploadConfig.getPath());
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        // 保存文件
        Path filePath = uploadPath.resolve(newFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        log.info("文件上传成功: {}", newFilename);
        return newFilename;
    }
    
    /**
     * 上传图片并转换为 Base64
     */
    public String uploadImageAsBase64(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("图片不能为空");
        }
        
        // 验证是否为图片
        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        if (!isImageType(extension)) {
            throw new IllegalArgumentException("不支持的图片类型: " + extension);
        }
        
        // 转换为 Base64
        byte[] bytes = file.getBytes();
        String base64 = Base64.getEncoder().encodeToString(bytes);
        
        // 添加 data URL 前缀
        String mimeType = file.getContentType();
        return "data:" + mimeType + ";base64," + base64;
    }
    
    /**
     * 读取文件内容
     */
    public String readFileContent(String filename) throws IOException {
        Path filePath = Paths.get(fileUploadConfig.getPath()).resolve(filename);
        if (!Files.exists(filePath)) {
            throw new IllegalArgumentException("文件不存在: " + filename);
        }
        return Files.readString(filePath);
    }
    
    /**
     * 读取文件字节
     */
    public byte[] readFileBytes(String filename) throws IOException {
        Path filePath = Paths.get(fileUploadConfig.getPath()).resolve(filename);
        if (!Files.exists(filePath)) {
            throw new IllegalArgumentException("文件不存在: " + filename);
        }
        return Files.readAllBytes(filePath);
    }
    
    /**
     * 删除文件
     */
    public void deleteFile(String filename) throws IOException {
        Path filePath = Paths.get(fileUploadConfig.getPath()).resolve(filename);
        if (Files.exists(filePath)) {
            Files.delete(filePath);
            log.info("文件删除成功: {}", filename);
        }
    }
    
    /**
     * 检查是否为允许的文件类型
     */
    private boolean isAllowedFileType(String extension) {
        if (extension == null) {
            return false;
        }
        extension = extension.toLowerCase();
        return isImageType(extension) || 
               Arrays.asList(fileUploadConfig.getFileTypes()).contains(extension);
    }
    
    /**
     * 检查是否为图片类型
     */
    private boolean isImageType(String extension) {
        if (extension == null) {
            return false;
        }
        extension = extension.toLowerCase();
        return Arrays.asList(fileUploadConfig.getImageTypes()).contains(extension);
    }
}
