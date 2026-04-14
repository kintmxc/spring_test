package com.example.spring_test.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("chat_message")
public class ChatMessage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sessionId;
    private Long farmerId;
    private Long consumerId;
    private Long senderUserId;
    private String type;
    private String content;
    private LocalDateTime time;
    private String avatar;
    private Boolean isRead;
    private LocalDateTime createdTime;
    private Integer isDeleted;
}
