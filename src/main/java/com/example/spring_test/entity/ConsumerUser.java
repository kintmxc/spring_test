package com.example.spring_test.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("consumer_user")
public class ConsumerUser {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String loginName;
    private String password;
    private String nickName;
    private String phone;
    private String avatar;
    private String address;
    private String description;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
