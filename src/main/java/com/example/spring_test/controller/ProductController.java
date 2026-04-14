package com.example.spring_test.controller;

import com.example.spring_test.common.PageResult;
import com.example.spring_test.common.Result;
import com.example.spring_test.dto.ProductQueryDTO;
import com.example.spring_test.dto.ProductSaveDTO;
import com.example.spring_test.dto.ProductSaleStatusUpdateDTO;
import com.example.spring_test.dto.ProductStockUpdateDTO;
import com.example.spring_test.service.ProductService;
import com.example.spring_test.vo.ProductDetailVO;
import com.example.spring_test.vo.ProductListVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public Result<PageResult<ProductListVO>> page(ProductQueryDTO productQueryDTO) {
        if (productQueryDTO.getPage() != null && productQueryDTO.getPage() > 0) {
            productQueryDTO.setPageNum(productQueryDTO.getPage());
        }
        return Result.success(productService.page(productQueryDTO));
    }

    @GetMapping("/{id}")
    public Result<ProductDetailVO> detail(@PathVariable Long id) {
        return Result.success(productService.detail(id));
    }

    @PostMapping
    public Result<ProductDetailVO> save(@Valid @RequestBody ProductSaveDTO productSaveDTO) {
        return Result.success("新增商品成功", productService.save(productSaveDTO));
    }

    @PutMapping("/{id}")
    public Result<ProductDetailVO> update(@PathVariable Long id, @Valid @RequestBody ProductSaveDTO productSaveDTO) {
        return Result.success("更新商品成功", productService.update(id, productSaveDTO));
    }

    @PutMapping("/{id}/sale-status")
    public Result<ProductDetailVO> updateSaleStatus(@PathVariable Long id,
                                                     @RequestBody ProductSaleStatusUpdateDTO productSaleStatusUpdateDTO) {
        return Result.success("更新商品状态成功", productService.updateSaleStatus(id, productSaleStatusUpdateDTO.getStatus()));
    }

    @PutMapping("/{id}/stock")
    public Result<ProductDetailVO> updateStock(@PathVariable Long id, @RequestBody ProductStockUpdateDTO productStockUpdateDTO) {
        return Result.success("更新商品库存成功", productService.updateStock(id, productStockUpdateDTO));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return Result.success("删除商品成功", null);
    }
}

@RestController
@RequestMapping("/api/farmer")
class FarmerProductController {
    private final ProductService productService;

    public FarmerProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/products")
    public Result<PageResult<ProductListVO>> myProducts(ProductQueryDTO productQueryDTO) {
        if (productQueryDTO.getPage() != null && productQueryDTO.getPage() > 0) {
            productQueryDTO.setPageNum(productQueryDTO.getPage());
        }
        return Result.success(productService.page(productQueryDTO));
    }
}
