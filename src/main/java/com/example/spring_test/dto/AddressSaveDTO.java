package com.example.spring_test.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddressSaveDTO {
    @NotBlank(message = "收货人不能为空")
    @JsonAlias({"receiverName", "receiver"})
    private String name;
    
    @NotBlank(message = "手机号不能为空")
    @JsonAlias({"receiverPhone"})
    private String phone;
    
    private String province;
    private String city;
    private String district;
    
    @JsonAlias({"detailAddress", "address"})
    private String detail;
    
    @JsonAlias({"isDefault"})
    private Boolean defaultFlag;
}
