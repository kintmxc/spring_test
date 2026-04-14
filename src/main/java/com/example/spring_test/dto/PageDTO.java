package com.example.spring_test.dto;

import lombok.Data;

@Data
public class PageDTO {
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private Integer page;
    
    public int getPageNum() {
        if (page != null && page > 0) {
            return page;
        }
        return pageNum != null && pageNum > 0 ? pageNum : 1;
    }
    
    public int getPageSize() {
        return pageSize != null && pageSize > 0 ? pageSize : 10;
    }
}
