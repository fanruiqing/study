package com.ai.assistant.mapper;

import com.ai.assistant.entity.ModelProvider;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ModelProviderMapper extends BaseMapper<ModelProvider> {
    
    @Select("SELECT * FROM model_providers WHERE is_active = 1")
    List<ModelProvider> findByIsActiveTrue();
    
    @Select("SELECT * FROM model_providers WHERE name = #{name}")
    ModelProvider findByName(@Param("name") String name);
}
