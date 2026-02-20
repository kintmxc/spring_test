package com.example.spring_test.entity;

import com.alibaba.druid.support.monitor.annotation.MTable;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("admin")
public class Admin {
//    主键ID
    @TableId(type = IdType.AUTO)
    private long id;
//    用户名
    private String username;
//    用户密码
    private String userpwd;
//    姓名
    private String name;
//    性别
    private String sex;
//    电话
    private String tel;
//    头像
    private String headurl;
}
