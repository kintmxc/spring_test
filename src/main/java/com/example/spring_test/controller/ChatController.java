package com.example.spring_test.controller;

import com.example.spring_test.common.Result;
import com.example.spring_test.service.ChatService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    
    @Resource
    private ChatService chatService;
    
    /**
     * 获取聊天列表
     */
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> getChatList(
            @RequestParam Long userId,
            @RequestParam String type
    ) {
        try {
            // 参数验证
            if (userId == null || userId <= 0) {
                return Result.fail("用户ID不能为空且必须大于0");
            }
            if (type == null) {
                return Result.fail("用户类型不能为空");
            }
            if (!"consumer".equals(type) && !"farmer".equals(type)) {
                return Result.fail("用户类型无效，只能是consumer或farmer");
            }
            
            List<Map<String, Object>> chatList = chatService.getChatList(userId, type)
                    .stream()
                    .map(session -> {
                        Map<String, Object> chatInfo = new java.util.HashMap<>();
                        chatInfo.put("id", session.getId());
                        chatInfo.put("farmerId", session.getFarmerId());
                        chatInfo.put("farmerName", session.getFarmerName());
                        chatInfo.put("consumerId", session.getConsumerId());
                        chatInfo.put("consumerName", session.getConsumerName());
                        chatInfo.put("lastMessage", session.getLastMessage());
                        chatInfo.put("lastMessageTime", session.getLastMessageTime());
                        chatInfo.put("unreadCount", session.getUnreadCount());
                        chatInfo.put("avatar", session.getAvatar());
                        return chatInfo;
                    })
                    .toList();
            
            return Result.success("获取聊天列表成功", chatList);
        } catch (Exception e) {
            return Result.fail("获取聊天列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取聊天记录
     */
    @GetMapping("/history")
    public Result<Map<String, Object>> getChatHistory(
            @RequestParam(required = false) Long farmerId,
            @RequestParam(required = false) Long consumerId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize
    ) {
        try {
            // 参数验证
            if ((farmerId == null || farmerId <= 0) && (consumerId == null || consumerId <= 0)) {
                return Result.fail("农户ID和消费者ID不能同时为空");
            }
            if (page == null || page < 1) {
                page = 1;
            }
            if (pageSize == null || pageSize < 1 || pageSize > 100) {
                pageSize = 20;
            }
            
            Map<String, Object> history = chatService.getChatHistory(farmerId, consumerId, page, pageSize);
            
            // 转换消息格式
            List<Map<String, Object>> messages = ((List<?>) history.get("list")).stream()
                    .map(msg -> {
                        Map<String, Object> messageInfo = new java.util.HashMap<>();
                        if (msg instanceof com.example.spring_test.entity.ChatMessage) {
                            com.example.spring_test.entity.ChatMessage chatMessage = (com.example.spring_test.entity.ChatMessage) msg;
                            messageInfo.put("id", chatMessage.getId());
                            messageInfo.put("type", chatMessage.getType());
                            messageInfo.put("content", chatMessage.getContent());
                            messageInfo.put("time", chatMessage.getTime().toString());
                            messageInfo.put("avatar", chatMessage.getAvatar());
                        }
                        return messageInfo;
                    })
                    .toList();
            
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("list", messages);
            result.put("total", history.get("total"));
            result.put("page", history.get("page"));
            result.put("pageSize", history.get("pageSize"));
            
            return Result.success("获取聊天记录成功", result);
        } catch (Exception e) {
            return Result.fail("获取聊天记录失败: " + e.getMessage());
        }
    }
    
    /**
     * 发送消息
     */
    @PostMapping("/send")
    public Result<Map<String, Object>> sendMessage(
            @RequestBody Map<String, Object> request
    ) {
        try {
            // 参数验证
            Long farmerId = null;
            Long consumerId = null;
            Object farmerIdObj = request.get("farmerId");
            Object consumerIdObj = request.get("consumerId");
            String content = (String) request.get("content");
            String type = (String) request.get("type");
            
            if (farmerIdObj instanceof Number) {
                farmerId = ((Number) farmerIdObj).longValue();
            }
            if (consumerIdObj instanceof Number) {
                consumerId = ((Number) consumerIdObj).longValue();
            }
            
            if (farmerId == null || farmerId <= 0) {
                return Result.fail("农户ID不能为空且必须大于0");
            }
            if (consumerId == null || consumerId <= 0) {
                return Result.fail("消费者ID不能为空且必须大于0");
            }
            
            // 从登录态获取当前用户ID
            Long currentUserId = com.example.spring_test.security.CurrentUserUtil.currentUserId();
            if (currentUserId == null) {
                return Result.fail("用户未登录");
            }
            
            // 校验：当前用户必须是会话双方之一
            if (!currentUserId.equals(farmerId) && !currentUserId.equals(consumerId)) {
                return Result.fail("当前用户不是该会话的参与者");
            }
            
            // 使用登录态的用户ID作为发送者ID（不信任客户端传的值）
            Long senderUserId = currentUserId;
            
            if (content == null || content.trim().isEmpty()) {
                return Result.fail("消息内容不能为空");
            }
            if (content.length() > 1000) {
                return Result.fail("消息内容不能超过1000字符");
            }
            if (type == null) {
                type = "text";
            }
            if (!"text".equals(type) && !"voice".equals(type) && !"image".equals(type)) {
                return Result.fail("消息类型无效，只能是text、voice或image");
            }
            
            com.example.spring_test.entity.ChatMessage message = chatService.sendMessage(farmerId, consumerId, senderUserId, content, type);
            
            Map<String, Object> messageInfo = new java.util.HashMap<>();
            messageInfo.put("id", message.getId());
            messageInfo.put("senderUserId", message.getSenderUserId());
            messageInfo.put("consumerId", message.getConsumerId());
            messageInfo.put("farmerId", message.getFarmerId());
            messageInfo.put("type", message.getType());
            messageInfo.put("content", message.getContent());
            messageInfo.put("time", message.getTime().toString());
            messageInfo.put("avatar", message.getAvatar());
            
            return Result.success("发送消息成功", messageInfo);
        } catch (Exception e) {
            return Result.fail("发送消息失败: " + e.getMessage());
        }
    }
    
    /**
     * AI聊天接口
     */
    @SuppressWarnings("unchecked")
    @PostMapping("/ai/chat")
    public Result<Map<String, Object>> aiChat(
            @RequestBody Map<String, Object> request
    ) {
        try {
            // 参数验证
            String content = (String) request.get("content");
            List<Map<String, String>> history = (List<Map<String, String>>) request.get("history");
            String type = (String) request.get("type");
            
            if (content == null || content.trim().isEmpty()) {
                return Result.fail("消息内容不能为空");
            }
            if (content.length() > 1000) {
                return Result.fail("消息内容不能超过1000字符");
            }
            if (type == null) {
                type = "consumer";
            }
            if (!"consumer".equals(type) && !"farmer".equals(type)) {
                return Result.fail("用户类型无效，只能是consumer或farmer");
            }
            
            String aiResponse = chatService.generateAIResponse(content, history, type);
            
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("content", aiResponse);
            
            return Result.success("获取AI回复成功", result);
        } catch (Exception e) {
            return Result.fail("获取AI回复失败: " + e.getMessage());
        }
    }
}
