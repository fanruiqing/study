package com.ai.assistant.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
@Service
public class DocumentProcessorService {
    
    /**
     * 提取文档文本内容
     */
    public String extractText(String filePath, String fileType) throws IOException {
        log.info("开始提取文档内容: {}, 类型: {}", filePath, fileType);
        
        String text;
        switch (fileType.toLowerCase()) {
            case "pdf":
                text = extractPdfText(filePath);
                break;
            case "docx":
                text = extractDocxText(filePath);
                break;
            case "txt":
            case "md":
                text = extractPlainText(filePath);
                break;
            default:
                throw new IllegalArgumentException("不支持的文件类型: " + fileType);
        }
        
        log.info("文档内容提取完成，长度: {} 字符", text.length());
        return text;
    }
    
    /**
     * 提取 PDF 文本
     */
    private String extractPdfText(String filePath) throws IOException {
        try (PDDocument document = PDDocument.load(new File(filePath))) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }
    
    /**
     * 提取 Word 文档文本
     */
    private String extractDocxText(String filePath) throws IOException {
        try (FileInputStream fis = new FileInputStream(filePath);
             XWPFDocument document = new XWPFDocument(fis)) {
            
            StringBuilder text = new StringBuilder();
            List<XWPFParagraph> paragraphs = document.getParagraphs();
            
            for (XWPFParagraph paragraph : paragraphs) {
                text.append(paragraph.getText()).append("\n");
            }
            
            return text.toString();
        }
    }
    
    /**
     * 提取纯文本文件内容
     */
    private String extractPlainText(String filePath) throws IOException {
        return new String(Files.readAllBytes(Paths.get(filePath)), "UTF-8");
    }
    
    /**
     * 检查是否支持该文件类型
     */
    public boolean supports(String fileType) {
        return fileType != null && 
               (fileType.equalsIgnoreCase("pdf") || 
                fileType.equalsIgnoreCase("docx") || 
                fileType.equalsIgnoreCase("txt") || 
                fileType.equalsIgnoreCase("md"));
    }
}
