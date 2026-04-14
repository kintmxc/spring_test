package com.example.spring_test.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class UserProfileUpdateDTO {
    @JsonAlias({"name", "farmerName", "nickName"})
    private String name;
    
    @JsonAlias({"phone", "contactPhone"})
    private String phone;
    
    @JsonAlias({"address", "originPlace"})
    private String address;
    
    private String description;
}
