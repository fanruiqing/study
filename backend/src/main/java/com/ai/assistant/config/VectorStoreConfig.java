package com.ai.assistant.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.nio.file.Paths;

@Configuration
public class VectorStoreConfig {
    
    @Value("${spring.ai.vectorstore.simple.path:uploads/vector-store}")
    private String vectorStorePath;
    
    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        // 创建向量存储目录
        File vectorStoreDir = new File(vectorStorePath);
        if (!vectorStoreDir.exists()) {
            vectorStoreDir.mkdirs();
        }
        
        // 创建 Simple Vector Store
        SimpleVectorStore vectorStore = new SimpleVectorStore(embeddingModel);
        
        // 尝试加载已存在的向量存储文件
        File vectorStoreFile = Paths.get(vectorStorePath, "vector-store.json").toFile();
        if (vectorStoreFile.exists()) {
            vectorStore.load(vectorStoreFile);
        }
        
        return vectorStore;
    }
}
