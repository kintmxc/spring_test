package com.example.spring_test.service.impl;

import com.example.spring_test.entity.ChatSession;
import com.example.spring_test.entity.ChatMessage;
import com.example.spring_test.mapper.ChatSessionMapper;
import com.example.spring_test.mapper.ChatMessageMapper;
import com.example.spring_test.service.ChatService;
import com.example.spring_test.ai.AiContentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ChatServiceImpl implements ChatService {
    
    private static final Logger log = LoggerFactory.getLogger(ChatServiceImpl.class);
    private static final int PRODUCT_INTRO_MAX_LENGTH = 70;
    private static final int PRODUCT_INTRO_MIN_LENGTH = 30;
    private static final Pattern PRODUCT_NAME_QUOTED = Pattern.compile("商品[\\\"“”']([^\\\"“”']+)[\\\"“”']");
    
    @Resource
    private ChatSessionMapper chatSessionMapper;
    
    @Resource
    private ChatMessageMapper chatMessageMapper;
    
    @Resource
    private AiContentService aiContentService;
    
    @Override
    public List<ChatSession> getChatList(Long userId, String type) {
        if ("farmer".equals(type)) {
            return chatSessionMapper.getSessionsWithNamesByFarmerId(userId);
        } else if ("consumer".equals(type)) {
            return chatSessionMapper.getSessionsWithNamesByConsumerId(userId);
        } else {
            return new ArrayList<>();
        }
    }
    
    @Override
    public Map<String, Object> getChatHistory(Long farmerId, Long consumerId, Integer page, Integer pageSize) {
        log.info("获取聊天记录 - farmerId: {}, consumerId: {}, page: {}, pageSize: {}", farmerId, consumerId, page, pageSize);
        
        Map<String, Object> result = new HashMap<>();
        
        // 参数校验
        if ((farmerId == null || farmerId <= 0) && (consumerId == null || consumerId <= 0)) {
            log.warn("获取聊天记录失败 - farmerId 和 consumerId 不能同时为空");
            result.put("list", new ArrayList<>());
            result.put("total", 0);
            result.put("page", page);
            result.put("pageSize", pageSize);
            return result;
        }
        
        // 获取会话 - 支持只传 farmerId 或只传 consumerId
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ChatSession> sessionWrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        if (farmerId != null && farmerId > 0) {
            sessionWrapper.eq("farmer_id", farmerId);
        }
        if (consumerId != null && consumerId > 0) {
            sessionWrapper.eq("consumer_id", consumerId);
        }
        
        List<ChatSession> sessions = chatSessionMapper.selectList(sessionWrapper);
        log.info("查询到 {} 个会话", sessions.size());
        
        if (sessions.isEmpty()) {
            log.warn("未找到会话 - farmerId: {}, consumerId: {}", farmerId, consumerId);
            result.put("list", new ArrayList<>());
            result.put("total", 0);
            result.put("page", page);
            result.put("pageSize", pageSize);
            return result;
        }
        
        // 如果找到多个会话，使用第一个
        ChatSession session = sessions.get(0);
        log.info("使用会话 ID: {}, farmerId: {}, consumerId: {}", 
                session.getId(), session.getFarmerId(), session.getConsumerId());
        
        // 获取消息列表
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ChatMessage> messageWrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        messageWrapper.eq("session_id", session.getId());
        messageWrapper.orderByDesc("time");
        
        // 计算分页
        int offset = (page - 1) * pageSize;
        messageWrapper.last("LIMIT " + pageSize + " OFFSET " + offset);
        
        List<ChatMessage> messages = chatMessageMapper.selectList(messageWrapper);
        log.info("查询到 {} 条消息", messages.size());
        
        // 获取消息总数
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ChatMessage> countWrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        countWrapper.eq("session_id", session.getId());
        Long total = chatMessageMapper.selectCount(countWrapper);
        
        result.put("list", messages);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);
        
        // 标记消息为已读
        if (consumerId != null && consumerId > 0) {
            markMessagesAsRead(session.getId(), consumerId, "consumer");
        }
        
        log.info("获取聊天记录成功 - 返回 {} 条消息，共 {} 条", messages.size(), total);
        return result;
    }
    
    @Override
    @Transactional
    public ChatMessage sendMessage(Long farmerId, Long consumerId, Long senderUserId, String content, String type) {
        log.info("发送消息 - farmerId: {}, consumerId: {}, senderUserId: {}", farmerId, consumerId, senderUserId);
        
        // 校验发送者必须是会话双方之一
        if (!senderUserId.equals(farmerId) && !senderUserId.equals(consumerId)) {
            throw new RuntimeException("发送者ID必须等于农户ID或消费者ID");
        }
        
        // 获取或创建会话
        ChatSession session = getOrCreateSession(farmerId, consumerId);
        
        LocalDateTime now = LocalDateTime.now();
        
        // 保存消息 - 使用自定义SQL插入，包含senderUserId
        chatMessageMapper.insertMessage(session.getId(), farmerId, consumerId, senderUserId, type, content, now, false, now);
        
        // 构建返回消息对象
        ChatMessage message = new ChatMessage();
        message.setSessionId(session.getId());
        message.setFarmerId(farmerId);
        message.setConsumerId(consumerId);
        message.setSenderUserId(senderUserId);
        message.setType(type);
        message.setContent(content);
        message.setTime(now);
        message.setIsRead(false);
        message.setCreatedTime(now);
        
        log.info("消息已保存 - sessionId: {}, senderUserId: {}", session.getId(), senderUserId);
        
        // 更新会话的最后消息信息
        ChatSession updateSession = new ChatSession();
        updateSession.setLastMessage(content);
        updateSession.setLastMessageSenderId(senderUserId);
        updateSession.setLastMessageTime(now);
        updateSession.setUnreadCount(session.getUnreadCount() + 1);
        updateSession.setUpdatedTime(now);
        
        com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<ChatSession> wrapper = new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<>();
        wrapper.eq("id", session.getId());
        chatSessionMapper.update(updateSession, wrapper);
        
        return message;
    }
    
    @Override
    public String generateAIResponse(String content, List<Map<String, String>> history, String type) {
        if (isFarmerProductIntroRequest(content, type)) {
            return generateFarmerProductIntro(content);
        }
        return aiContentService.generateChatResponse(content, history, type);
    }

    private boolean isFarmerProductIntroRequest(String content, String type) {
        if (!"farmer".equalsIgnoreCase(type) || content == null) {
            return false;
        }
        String normalized = content.replace(" ", "");
        return (normalized.contains("商品介绍") || normalized.contains("商品文案") || normalized.contains("生成一段"))
                && normalized.contains("商品");
    }

    private String generateFarmerProductIntro(String originalContent) {
        String productName = extractProductName(originalContent);
        String prompt = buildFarmerProductIntroPrompt(productName, originalContent);
        String response = aiContentService.generateChatResponse(prompt, Collections.emptyList(), "farmer");
        String normalized = normalizeProductIntro(response);

        // If the model returns text that is too short, retry once with stricter guidance.
        if (normalized.length() < PRODUCT_INTRO_MIN_LENGTH) {
            String retryPrompt = prompt + "\n请直接输出一段35-60字商品介绍，不要解释。";
            String retry = aiContentService.generateChatResponse(retryPrompt, Collections.emptyList(), "farmer");
            normalized = normalizeProductIntro(retry);
        }
        return normalized;
    }

    private String extractProductName(String content) {
        Matcher matcher = PRODUCT_NAME_QUOTED.matcher(content);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "该商品";
    }

    private String buildFarmerProductIntroPrompt(String productName, String originalContent) {
        return "你是农产品电商文案助手。请为商品生成一段适合电商详情页的介绍。\n"
                + "要求：中文、约50字、突出2-3个真实卖点、简洁明了、避免夸大宣传和医疗功效。\n"
                + "商品名称：" + productName + "\n"
                + "用户原始需求：" + originalContent + "\n"
                + "输出格式：仅输出最终商品介绍正文，不要加标题或解释。";
    }

    private String normalizeProductIntro(String content) {
        if (content == null || content.isBlank()) {
            return "该商品选材讲究，品质稳定，口感自然，适合家庭日常消费场景。";
        }
        String text = content.replaceAll("\\s+", "").trim();
        if (text.length() > PRODUCT_INTRO_MAX_LENGTH) {
            text = text.substring(0, PRODUCT_INTRO_MAX_LENGTH);
        }
        if (!text.endsWith("。") && !text.endsWith("！") && !text.endsWith("？")) {
            text = text + "。";
        }
        return text;
    }
    
    @Override
    public void markMessagesAsRead(Long sessionId, Long userId, String userType) {
        ChatMessage updateMessage = new ChatMessage();
        updateMessage.setIsRead(true);
        
        com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<ChatMessage> wrapper = new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<>();
        wrapper.eq("session_id", sessionId);
        
        chatMessageMapper.update(updateMessage, wrapper);
    }
    
    @Override
    public ChatSession getOrCreateSession(Long farmerId, Long consumerId) {
        // 查找现有会话
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ChatSession> wrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        wrapper.eq("farmer_id", farmerId).eq("consumer_id", consumerId);
        ChatSession session = chatSessionMapper.selectOne(wrapper);
        
        if (session == null) {
            // 创建新会话
            LocalDateTime now = LocalDateTime.now();
            chatSessionMapper.insertSession(farmerId, consumerId, 0, now, now);
            
            // 重新查询会话以获取ID
            com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ChatSession> newWrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
            newWrapper.eq("farmer_id", farmerId).eq("consumer_id", consumerId);
            session = chatSessionMapper.selectOne(newWrapper);
        }
        
        return session;
    }
}
