package com.ai.assistant.mapper;

import com.ai.assistant.entity.Session;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SessionMapper extends BaseMapper<Session> {
    
    @Select("SELECT * FROM sessions ORDER BY updated_at DESC")
    List<Session> findAllByOrderByUpdatedAtDesc();
    
    @Select("SELECT * FROM sessions WHERE title LIKE CONCAT('%', #{query}, '%')")
    List<Session> searchByTitle(@Param("query") String query);
}
