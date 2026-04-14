package com.example.spring_test.service;

import com.example.spring_test.entity.ChatSession;
import com.example.spring_test.entity.ChatMessage;
import java.util.List;
import java.util.Map;

public interface ChatService {
    
    /**
     * 获取聊天会话列表
     */
    List<ChatSession> getChatList(Long userId, String type);
    
    /**
     * 获取聊天记录
     */
    Map<String, Object> getChatHistory(Long farmerId, Long consumerId, Integer page, Integer pageSize);
    
    /**
     * 发送消息
     */
    ChatMessage sendMessage(Long farmerId, Long consumerId, Long senderUserId, String content, String type);
    
    /**
     * 生成AI回复
     */
    String generateAIResponse(String content, List<Map<String, String>> history, String type);
    
    /**
     * 标记消息为已读
     */
    void markMessagesAsRead(Long sessionId, Long userId, String userType);
    
    /**
     * 获取或创建会话
     */
    ChatSession getOrCreateSession(Long farmerId, Long consumerId);
}
