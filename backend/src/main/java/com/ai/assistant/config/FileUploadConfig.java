package com.ai.assistant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "file.upload")
public class FileUploadConfig {
    
    /**
     * 文件上传路径
     */
    private String path = "uploads";
    
    /**
     * 最大文件大小（MB）
     */
    private Long maxSize = 10L;
    
    /**
     * 允许的图片格式
     */
    private String[] imageTypes = {"jpg", "jpeg", "png", "gif", "bmp", "webp"};
    
    /**
     * 允许的文件格式
     */
    private String[] fileTypes = {"txt", "pdf", "doc", "docx", "xls", "xlsx", "csv", "json", "xml", "md"};
}
