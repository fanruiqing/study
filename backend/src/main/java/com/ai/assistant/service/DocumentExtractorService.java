package com.ai.assistant.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * 文档内容提取服务
 * 支持 PDF、Word、Excel、TXT 等格式
 */
@Slf4j
@Service
public class DocumentExtractorService {
    
    /**
     * 提取文档内容
     */
    public String extractContent(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        
        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        
        log.info("开始提取文档内容: {} ({})", filename, extension);
        
        return switch (extension) {
            case "pdf" -> extractPdf(file.getInputStream());
            case "doc", "docx" -> extractWord(file.getInputStream());
            case "xls", "xlsx" -> extractExcel(file.getInputStream());
            case "txt", "md", "json", "xml", "csv" -> extractText(file.getInputStream());
            default -> throw new IllegalArgumentException("不支持的文件格式: " + extension);
        };
    }
    
    /**
     * 提取 PDF 内容
     */
    private String extractPdf(InputStream inputStream) throws IOException {
        try (PDDocument document = PDDocument.load(inputStream)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            log.info("✓ PDF 提取成功，共 {} 页，{} 字符", document.getNumberOfPages(), text.length());
            return text;
        }
    }
    
    /**
     * 提取 Word 内容
     */
    private String extractWord(InputStream inputStream) throws IOException {
        try (XWPFDocument document = new XWPFDocument(inputStream)) {
            StringBuilder text = new StringBuilder();
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                text.append(paragraph.getText()).append("\n");
            }
            log.info("✓ Word 提取成功，{} 字符", text.length());
            return text.toString();
        }
    }
    
    /**
     * 提取 Excel 内容
     */
    private String extractExcel(InputStream inputStream) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            StringBuilder text = new StringBuilder();
            
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                text.append("=== ").append(sheet.getSheetName()).append(" ===\n");
                
                for (Row row : sheet) {
                    for (Cell cell : row) {
                        text.append(getCellValue(cell)).append("\t");
                    }
                    text.append("\n");
                }
                text.append("\n");
            }
            
            log.info("✓ Excel 提取成功，{} 个工作表，{} 字符", 
                    workbook.getNumberOfSheets(), text.length());
            return text.toString();
        }
    }
    
    /**
     * 提取文本文件内容
     */
    private String extractText(InputStream inputStream) throws IOException {
        String text = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        log.info("✓ 文本提取成功，{} 字符", text.length());
        return text;
    }
    
    /**
     * 获取单元格值
     */
    private String getCellValue(Cell cell) {
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getDateCellValue().toString();
                } else {
                    yield String.valueOf(cell.getNumericCellValue());
                }
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }
}
