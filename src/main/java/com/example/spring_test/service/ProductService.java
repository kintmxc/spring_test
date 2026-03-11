package com.example.spring_test.service;

import com.example.spring_test.common.PageResult;
import com.example.spring_test.dto.ProductQueryDTO;
import com.example.spring_test.dto.ProductSaveDTO;
import com.example.spring_test.dto.ProductStockUpdateDTO;
import com.example.spring_test.vo.ProductDetailVO;
import com.example.spring_test.vo.ProductListVO;

public interface ProductService {
    PageResult<ProductListVO> page(ProductQueryDTO productQueryDTO);

    ProductDetailVO detail(Long id);

    ProductDetailVO save(ProductSaveDTO productSaveDTO);

    ProductDetailVO update(Long id, ProductSaveDTO productSaveDTO);

    ProductDetailVO updateSaleStatus(Long id, Integer saleStatus);

    ProductDetailVO updateStock(Long id, ProductStockUpdateDTO productStockUpdateDTO);

    void delete(Long id);
}