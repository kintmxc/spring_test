package com.example.spring_test.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class FarmerDetailVO {
    private Long id;
    private String loginName;
    private String farmerName;
    private String contactPhone;
    private String originPlace;
    private String idCardNo;
    private String licenseNo;
    private Integer authStatus;
    private String authStatusText;
    private Integer accountStatus;
    private String accountStatusText;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer productCount;
    private Integer onSaleProductCount;
    private Integer orderCount;
}