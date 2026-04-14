package com.example.spring_test.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.spring_test.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
    
    /**
     * 根据会话ID获取聊天消息列表
     */
    List<ChatMessage> getMessagesBySessionId(@Param("sessionId") Long sessionId, @Param("page") Integer page, @Param("pageSize") Integer pageSize);
    
    /**
     * 根据农户ID和消费者ID获取聊天消息列表
     */
    List<ChatMessage> getMessagesByFarmerAndConsumer(
            @Param("farmerId") Long farmerId, 
            @Param("consumerId") Long consumerId, 
            @Param("page") Integer page, 
            @Param("pageSize") Integer pageSize
    );
    
    /**
     * 获取消息总数
     */
    int getMessageCount(@Param("farmerId") Long farmerId, @Param("consumerId") Long consumerId);
    
    /**
     * 标记消息为已读
     */
    int markMessagesAsRead(@Param("sessionId") Long sessionId, @Param("userId") Long userId, @Param("userType") String userType);
    
    /**
     * 插入聊天消息
     */
    int insertMessage(@Param("sessionId") Long sessionId, 
                      @Param("farmerId") Long farmerId, 
                      @Param("consumerId") Long consumerId,
                      @Param("senderUserId") Long senderUserId,
                      @Param("type") String type, 
                      @Param("content") String content, 
                      @Param("time") java.time.LocalDateTime time, 
                      @Param("isRead") Boolean isRead, 
                      @Param("createdTime") java.time.LocalDateTime createdTime);
}
