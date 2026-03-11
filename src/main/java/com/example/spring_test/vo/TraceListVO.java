package com.example.spring_test.vo;

import java.time.LocalDate;
import lombok.Data;

@Data
public class TraceListVO {
    private Long traceId;
    private Long productId;
    private Long farmerId;
    private Long categoryId;
    private String productName;
    private String farmerName;
    private String categoryName;
    private String originPlace;
    private String coverImage;
    private Integer stock;
    private Integer saleStatus;
    private LocalDate productionDate;
    private String originDesc;
    private String inspectDesc;
    private Integer traceStatus;
    private boolean traceMaintained;
}