package com.example.spring_test.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("farmer")
public class Farmer {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String loginName;
    private String password;
    private String farmerName;
    private String contactPhone;
    private String originPlace;
    private String idCardNo;
    private String licenseNo;
    private Integer authStatus;
    private Integer accountStatus;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}