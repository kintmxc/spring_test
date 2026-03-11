package com.example.spring_test.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class FarmerListVO {
    private Long id;
    private String loginName;
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
    private String authStatusText;
    private String accountStatusText;
    private Integer productCount;
    private Integer onSaleProductCount;
    private Integer orderCount;
}