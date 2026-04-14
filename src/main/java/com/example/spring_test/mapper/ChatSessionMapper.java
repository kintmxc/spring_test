package com.example.spring_test.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.spring_test.entity.ChatSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSession> {
    
    /**
     * 根据消费者ID获取聊天会话列表
     */
    List<ChatSession> getSessionsByConsumerId(@Param("consumerId") Long consumerId);
    
    /**
     * 根据农户ID获取聊天会话列表
     */
    List<ChatSession> getSessionsByFarmerId(@Param("farmerId") Long farmerId);
    
    /**
     * 根据农户ID和消费者ID获取会话
     */
    ChatSession getSessionByFarmerAndConsumer(@Param("farmerId") Long farmerId, @Param("consumerId") Long consumerId);
    
    /**
     * 更新会话的最后消息信息
     */
    int updateLastMessage(@Param("id") Long id, @Param("lastMessage") String lastMessage, @Param("lastMessageTime") java.time.LocalDateTime lastMessageTime);
    
    /**
     * 更新未读消息数
     */
    int updateUnreadCount(@Param("id") Long id, @Param("unreadCount") Integer unreadCount);
    
    /**
     * 插入聊天会话
     */
    int insertSession(@Param("farmerId") Long farmerId, @Param("consumerId") Long consumerId, @Param("unreadCount") Integer unreadCount, @Param("createdTime") java.time.LocalDateTime createdTime, @Param("updatedTime") java.time.LocalDateTime updatedTime);
    
    /**
     * 根据消费者ID获取聊天会话列表（包含农户名称）
     */
    List<ChatSession> getSessionsWithNamesByConsumerId(@Param("consumerId") Long consumerId);
    
    /**
     * 根据农户ID获取聊天会话列表（包含消费者名称）
     */
    List<ChatSession> getSessionsWithNamesByFarmerId(@Param("farmerId") Long farmerId);
}
