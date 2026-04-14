package com.example.spring_test.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("chat_session")
public class ChatSession {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long farmerId;
    private String farmerName;
    private Long consumerId;
    private String consumerName;
    private String lastMessage;
    private Long lastMessageSenderId;
    @TableField("last_message_time")
    private LocalDateTime lastMessageTime;
    private Integer unreadCount;
    private String avatar;
    @TableField("created_time")
    private LocalDateTime createdTime;
    @TableField("updated_time")
    private LocalDateTime updatedTime;
    private Integer isDeleted;
}
