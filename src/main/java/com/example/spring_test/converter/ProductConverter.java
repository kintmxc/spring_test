package com.example.spring_test.converter;

import com.example.spring_test.entity.Farmer;
import com.example.spring_test.entity.Product;
import com.example.spring_test.entity.ProductCategory;
import com.example.spring_test.entity.ProductTrace;
import com.example.spring_test.vo.ProductDetailVO;
import com.example.spring_test.vo.ProductListVO;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ProductConverter {

    public ProductDetailVO toDetailVO(Product product, ProductCategory category, 
                                       Farmer farmer, ProductTrace trace) {
        ProductDetailVO vo = new ProductDetailVO();
        vo.setId(product.getId());
        vo.setFarmerId(product.getFarmerId());
        vo.setCategoryId(product.getCategoryId());
        vo.setProductName(product.getProductName());
        vo.setPrice(product.getPrice());
        vo.setStock(product.getStock());
        vo.setUnitName(product.getUnitName());
        vo.setOriginPlace(product.getOriginPlace());
        vo.setCoverImage(product.getCoverImage());
        vo.setDescription(product.getDescription());
        vo.setSaleStatus(product.getSaleStatus());
        vo.setSalesCount(product.getSalesCount());
        vo.setCreateTime(product.getCreateTime());
        vo.setUpdateTime(product.getUpdateTime());
        vo.setCategoryName(category == null ? "-" : category.getCategoryName());
        vo.setFarmerName(farmer == null ? "-" : farmer.getFarmerName());
        vo.setTraceMaintained(trace != null);
        if (trace != null) {
            vo.setTraceId(trace.getId());
            vo.setProductionDate(trace.getProductionDate());
            vo.setOriginDesc(trace.getOriginDesc());
            vo.setInspectDesc(trace.getInspectDesc());
            vo.setTraceStatus(trace.getTraceStatus());
            vo.setTraceStatusText(trace.getTraceStatus() != null && trace.getTraceStatus() == 1 ? "有效" : "停用");
        } else {
            vo.setTraceStatusText("未维护");
        }
        return vo;
    }

    public ProductListVO toListVO(Product product, Map<Long, String> categoryNameMap, 
                                   Map<Long, String> farmerNameMap) {
        ProductListVO vo = new ProductListVO();
        vo.setId(product.getId());
        vo.setFarmerId(product.getFarmerId());
        vo.setCategoryId(product.getCategoryId());
        vo.setProductName(product.getProductName());
        vo.setPrice(product.getPrice());
        vo.setStock(product.getStock());
        vo.setUnitName(product.getUnitName());
        vo.setOriginPlace(product.getOriginPlace());
        vo.setCoverImage(product.getCoverImage());
        vo.setDescription(product.getDescription());
        vo.setSaleStatus(product.getSaleStatus());
        vo.setSalesCount(product.getSalesCount());
        vo.setCreateTime(product.getCreateTime());
        vo.setUpdateTime(product.getUpdateTime());
        vo.setCategoryName(categoryNameMap.getOrDefault(product.getCategoryId(), "-"));
        vo.setFarmerName(farmerNameMap.getOrDefault(product.getFarmerId(), "-"));
        return vo;
    }
}
