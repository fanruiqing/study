package com.ai.assistant.mapper;

import com.ai.assistant.entity.PromptTemplate;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PromptTemplateMapper extends BaseMapper<PromptTemplate> {
    
    @Select("SELECT * FROM prompt_templates WHERE category = #{category}")
    List<PromptTemplate> findByCategory(@Param("category") String category);
    
    @Select("SELECT DISTINCT category FROM prompt_templates WHERE category IS NOT NULL")
    List<String> findAllCategories();
    
    @Select("SELECT * FROM prompt_templates WHERE title LIKE CONCAT('%', #{query}, '%') OR content LIKE CONCAT('%', #{query}, '%')")
    List<PromptTemplate> search(@Param("query") String query);
}
