package com.ai.assistant.mapper;

import com.ai.assistant.entity.Message;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {
    
    @Select("SELECT * FROM messages WHERE session_id = #{sessionId} ORDER BY timestamp ASC")
    List<Message> findBySessionIdOrderByTimestampAsc(@Param("sessionId") String sessionId);
    
    @Delete("DELETE FROM messages WHERE session_id = #{sessionId}")
    int deleteBySessionId(@Param("sessionId") String sessionId);
    
    @Delete("DELETE FROM messages WHERE session_id = #{sessionId} AND timestamp >= #{timestamp}")
    int deleteBySessionIdAndTimestampGreaterThanEqual(@Param("sessionId") String sessionId, @Param("timestamp") Long timestamp);
    
    @Select("SELECT * FROM messages WHERE session_id = #{sessionId} AND content LIKE CONCAT('%', #{query}, '%')")
    List<Message> searchByContent(@Param("sessionId") String sessionId, @Param("query") String query);
    
    @Select("SELECT COUNT(*) FROM messages WHERE session_id = #{sessionId}")
    long countBySessionId(@Param("sessionId") String sessionId);
}
