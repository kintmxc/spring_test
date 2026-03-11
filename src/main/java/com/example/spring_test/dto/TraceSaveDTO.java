package com.example.spring_test.dto;

import java.time.LocalDate;
import lombok.Data;

@Data
public class TraceSaveDTO {
    private Long productId;
    private LocalDate productionDate;
    private String originDesc;
    private String inspectDesc;
    private Integer traceStatus;
}