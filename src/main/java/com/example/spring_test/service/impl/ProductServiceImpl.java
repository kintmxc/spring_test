package com.example.spring_test.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.spring_test.common.PageResult;
import com.example.spring_test.dto.ProductQueryDTO;
import com.example.spring_test.dto.ProductSaveDTO;
import com.example.spring_test.dto.ProductStockUpdateDTO;
import com.example.spring_test.entity.Farmer;
import com.example.spring_test.entity.OrderItem;
import com.example.spring_test.entity.Product;
import com.example.spring_test.entity.ProductCategory;
import com.example.spring_test.entity.ProductTrace;
import com.example.spring_test.exception.BusinessException;
import com.example.spring_test.exception.ForbiddenException;
import com.example.spring_test.mapper.FarmerMapper;
import com.example.spring_test.mapper.OrderItemMapper;
import com.example.spring_test.mapper.ProductCategoryMapper;
import com.example.spring_test.mapper.ProductMapper;
import com.example.spring_test.mapper.ProductTraceMapper;
import com.example.spring_test.security.CurrentUserUtil;
import com.example.spring_test.service.ProductService;
import com.example.spring_test.vo.ProductDetailVO;
import com.example.spring_test.vo.ProductListVO;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductServiceImpl implements ProductService {
    private final ProductMapper productMapper;
    private final ProductCategoryMapper productCategoryMapper;
    private final FarmerMapper farmerMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductTraceMapper productTraceMapper;

    public ProductServiceImpl(ProductMapper productMapper,
                              ProductCategoryMapper productCategoryMapper,
                              FarmerMapper farmerMapper,
                              OrderItemMapper orderItemMapper,
                              ProductTraceMapper productTraceMapper) {
        this.productMapper = productMapper;
        this.productCategoryMapper = productCategoryMapper;
        this.farmerMapper = farmerMapper;
        this.orderItemMapper = orderItemMapper;
        this.productTraceMapper = productTraceMapper;
    }

    @Override
    public PageResult<ProductListVO> page(ProductQueryDTO productQueryDTO) {
        Page<Product> page = new Page<>(productQueryDTO.getPageNum(), productQueryDTO.getPageSize());
        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<Product>()
                .like(productQueryDTO.getProductName() != null && !productQueryDTO.getProductName().isBlank(), Product::getProductName, productQueryDTO.getProductName())
                .like(productQueryDTO.getOriginPlace() != null && !productQueryDTO.getOriginPlace().isBlank(), Product::getOriginPlace, productQueryDTO.getOriginPlace())
                .eq(productQueryDTO.getCategoryId() != null, Product::getCategoryId, productQueryDTO.getCategoryId())
                .eq(resolveFarmerId(productQueryDTO.getFarmerId()) != null, Product::getFarmerId, resolveFarmerId(productQueryDTO.getFarmerId()))
                .eq(productQueryDTO.getSaleStatus() != null, Product::getSaleStatus, productQueryDTO.getSaleStatus())
                .orderByDesc(Product::getId);
        applyStockFilter(queryWrapper, productQueryDTO.getStockStatus());
        if (productQueryDTO.getHasCoverImage() != null) {
            if (productQueryDTO.getHasCoverImage() == 1) {
                queryWrapper.isNotNull(Product::getCoverImage).ne(Product::getCoverImage, "");
            } else if (productQueryDTO.getHasCoverImage() == 0) {
                queryWrapper.and(wrapper -> wrapper.isNull(Product::getCoverImage).or().eq(Product::getCoverImage, ""));
            }
        }
        Page<Product> result = productMapper.selectPage(page, queryWrapper);
        List<ProductListVO> records = buildProductList(result.getRecords());
        return new PageResult<>(result.getTotal(), result.getCurrent(), result.getSize(), records);
    }

    @Override
    public ProductDetailVO detail(Long id) {
        Product product = getProduct(id);
        return buildProductDetail(product);
    }

    @Override
    public ProductDetailVO save(ProductSaveDTO productSaveDTO) {
        Long farmerId = resolveWritableFarmerId(productSaveDTO.getFarmerId());
        validateReferences(productSaveDTO.getCategoryId(), farmerId);
        Product product = new Product();
        productSaveDTO.setFarmerId(farmerId);
        fillProduct(product, productSaveDTO);
        product.setSalesCount(0);
        productMapper.insert(product);
        return buildProductDetail(product);
    }

    @Override
    public ProductDetailVO update(Long id, ProductSaveDTO productSaveDTO) {
        Product product = getProduct(id);
        Long farmerId = resolveWritableFarmerId(productSaveDTO.getFarmerId());
        validateReferences(productSaveDTO.getCategoryId(), farmerId);
        productSaveDTO.setFarmerId(farmerId);
        fillProduct(product, productSaveDTO);
        productMapper.updateById(product);
        return buildProductDetail(product);
    }

    @Override
    public ProductDetailVO updateSaleStatus(Long id, Integer saleStatus) {
        Product product = getProduct(id);
        product.setSaleStatus(saleStatus);
        productMapper.updateById(product);
        return buildProductDetail(product);
    }

    @Override
    public ProductDetailVO updateStock(Long id, ProductStockUpdateDTO productStockUpdateDTO) {
        Product product = getProduct(id);
        if (productStockUpdateDTO.getStock() == null || productStockUpdateDTO.getStock() < 0) {
            throw new BusinessException("库存必须为大于等于0的整数");
        }
        product.setStock(productStockUpdateDTO.getStock());
        productMapper.updateById(product);
        return buildProductDetail(product);
    }

    private ProductDetailVO buildProductDetail(Product product) {
        ProductCategory category = productCategoryMapper.selectById(product.getCategoryId());
        Farmer farmer = farmerMapper.selectById(product.getFarmerId());
        ProductTrace trace = productTraceMapper.selectOne(new LambdaQueryWrapper<ProductTrace>()
                .eq(ProductTrace::getProductId, product.getId())
                .last("limit 1"));
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

    @Override
    @Transactional
    public void delete(Long id) {
        Product product = getProduct(id);
        Long orderItemCount = orderItemMapper.selectCount(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getProductId, product.getId()));
        if (orderItemCount != null && orderItemCount > 0) {
            throw new BusinessException("商品已有关联订单，不能删除");
        }
        ProductTrace trace = productTraceMapper.selectOne(new LambdaQueryWrapper<ProductTrace>()
                .eq(ProductTrace::getProductId, product.getId())
                .last("limit 1"));
        if (trace != null) {
            productTraceMapper.deleteById(trace.getId());
        }
        productMapper.deleteById(product.getId());
    }

    private Product getProduct(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        CurrentUserUtil.requireAdminOrOwner(product.getFarmerId(), "无权访问该商品");
        return product;
    }

    private Long resolveFarmerId(Long queryFarmerId) {
        if (CurrentUserUtil.isFarmer()) {
            return CurrentUserUtil.currentUserId();
        }
        return queryFarmerId;
    }

    private Long resolveWritableFarmerId(Long farmerId) {
        if (CurrentUserUtil.isFarmer()) {
            if (farmerId != null && !CurrentUserUtil.currentUserId().equals(farmerId)) {
                throw new ForbiddenException("农户只能新增或编辑自己的商品，不能指定其他农户");
            }
            return CurrentUserUtil.currentUserId();
        }
        return farmerId;
    }

    private void validateReferences(Long categoryId, Long farmerId) {
        if (categoryId == null || productCategoryMapper.selectById(categoryId) == null) {
            throw new BusinessException("商品分类不存在");
        }
        if (farmerId == null) {
            throw new BusinessException("所属农户不能为空");
        }
        Farmer farmer = farmerMapper.selectById(farmerId);
        if (farmer == null) {
            throw new BusinessException("所属农户不存在");
        }
        if (farmer.getAccountStatus() != null && farmer.getAccountStatus() == 0) {
            throw new BusinessException("所属农户已被禁用");
        }
    }

    private void applyStockFilter(LambdaQueryWrapper<Product> queryWrapper, Integer stockStatus) {
        if (stockStatus == null) {
            return;
        }
        if (stockStatus == 0) {
            queryWrapper.eq(Product::getStock, 0);
        } else if (stockStatus == 1) {
            queryWrapper.gt(Product::getStock, 0).le(Product::getStock, 20);
        } else if (stockStatus == 2) {
            queryWrapper.gt(Product::getStock, 20);
        }
    }

    private List<ProductListVO> buildProductList(List<Product> products) {
        if (products == null || products.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> categoryIds = products.stream()
                .map(Product::getCategoryId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Set<Long> farmerIds = products.stream()
                .map(Product::getFarmerId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        Map<Long, String> categoryNameMap = categoryIds.isEmpty()
                ? Collections.emptyMap()
                : productCategoryMapper.selectBatchIds(categoryIds).stream()
                .collect(Collectors.toMap(ProductCategory::getId, ProductCategory::getCategoryName));
        Map<Long, String> farmerNameMap = farmerIds.isEmpty()
                ? Collections.emptyMap()
                : farmerMapper.selectBatchIds(farmerIds).stream()
                .collect(Collectors.toMap(Farmer::getId, Farmer::getFarmerName));

        return products.stream().map(product -> toListVO(product, categoryNameMap, farmerNameMap)).collect(Collectors.toList());
    }

    private ProductListVO toListVO(Product product, Map<Long, String> categoryNameMap, Map<Long, String> farmerNameMap) {
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

    private void fillProduct(Product product, ProductSaveDTO dto) {
        if (dto.getProductName() == null || dto.getProductName().isBlank()) {
            throw new BusinessException("商品名称不能为空");
        }
        product.setFarmerId(dto.getFarmerId());
        product.setCategoryId(dto.getCategoryId());
        product.setProductName(dto.getProductName());
        product.setPrice(dto.getPrice());
        product.setStock(dto.getStock());
        product.setUnitName(dto.getUnitName());
        product.setOriginPlace(dto.getOriginPlace());
        product.setCoverImage(dto.getCoverImage());
        product.setDescription(dto.getDescription());
        product.setSaleStatus(dto.getSaleStatus() == null ? 1 : dto.getSaleStatus());
    }
}