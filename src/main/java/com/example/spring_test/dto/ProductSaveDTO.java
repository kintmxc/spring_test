package com.example.spring_test.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Data;

@Data
public class ProductSaveDTO {
    private Long farmerId;
    
    @NotNull(message = "分类ID不能为空")
    private Long categoryId;
    
    @NotBlank(message = "商品名称不能为空")
    @JsonAlias({"name", "productName"})
    private String productName;
    
    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.01", message = "价格必须大于0")
    private BigDecimal price;
    
    @NotNull(message = "库存不能为空")
    @Min(value = 0, message = "库存不能为负数")
    private Integer stock;
    
    @JsonAlias({"unit"})
    private String unitName;
    
    @JsonAlias({"origin"})
    private String originPlace;
    
    private String coverImage;
    
    private List<String> images;
    
    private String description;
    
    private Integer saleStatus;
    
    @JsonAlias({"productionTime"})
    private LocalDate productionDate;
    
    @JsonAlias({"inspection"})
    private String inspectDesc;
}
