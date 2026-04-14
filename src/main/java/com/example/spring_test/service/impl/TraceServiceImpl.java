package com.example.spring_test.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.spring_test.common.PageResult;
import com.example.spring_test.dto.TraceQueryDTO;
import com.example.spring_test.dto.TraceSaveDTO;
import com.example.spring_test.entity.Farmer;
import com.example.spring_test.entity.Product;
import com.example.spring_test.entity.ProductCategory;
import com.example.spring_test.entity.ProductTrace;
import com.example.spring_test.exception.BusinessException;
import com.example.spring_test.mapper.FarmerMapper;
import com.example.spring_test.mapper.ProductCategoryMapper;
import com.example.spring_test.mapper.ProductMapper;
import com.example.spring_test.mapper.ProductTraceMapper;
import com.example.spring_test.security.CurrentUserUtil;
import com.example.spring_test.service.TraceService;
import com.example.spring_test.util.UrlUtils;
import com.example.spring_test.vo.TraceListVO;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TraceServiceImpl implements TraceService {
    private final ProductTraceMapper productTraceMapper;
    private final ProductMapper productMapper;
    private final FarmerMapper farmerMapper;
    private final ProductCategoryMapper productCategoryMapper;

    public TraceServiceImpl(ProductTraceMapper productTraceMapper,
                            ProductMapper productMapper,
                            FarmerMapper farmerMapper,
                            ProductCategoryMapper productCategoryMapper) {
        this.productTraceMapper = productTraceMapper;
        this.productMapper = productMapper;
        this.farmerMapper = farmerMapper;
        this.productCategoryMapper = productCategoryMapper;
    }

    @Override
    public PageResult<TraceListVO> page(TraceQueryDTO traceQueryDTO) {
        Long farmerId = CurrentUserUtil.isFarmer() ? CurrentUserUtil.currentUserId() : traceQueryDTO.getFarmerId();
        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<Product>()
                .like(traceQueryDTO.getProductName() != null && !traceQueryDTO.getProductName().isBlank(), Product::getProductName, traceQueryDTO.getProductName())
                .eq(farmerId != null, Product::getFarmerId, farmerId)
                .orderByDesc(Product::getId);
        List<Product> products = productMapper.selectList(queryWrapper);
        List<TraceListVO> allRecords = buildTraceList(products, traceQueryDTO.getTraceStatus());

        int fromIndex = (int) Math.max(0, (traceQueryDTO.getPageNum() - 1) * traceQueryDTO.getPageSize());
        int toIndex = Math.min(allRecords.size(), fromIndex + (int) traceQueryDTO.getPageSize());
        List<TraceListVO> pageRecords = fromIndex >= allRecords.size() ? Collections.emptyList() : allRecords.subList(fromIndex, toIndex);
        return new PageResult<>(allRecords.size(), traceQueryDTO.getPageNum(), traceQueryDTO.getPageSize(), pageRecords);
    }

    @Override
    public TraceListVO getByProductId(Long productId) {
        Product product = validateProduct(productId);
        return buildTraceDetail(product);
    }

    @Override
    @Transactional
    public TraceListVO saveOrUpdate(TraceSaveDTO traceSaveDTO) {
        Product product = validateProduct(traceSaveDTO.getProductId());
        ProductTrace trace = productTraceMapper.selectOne(new LambdaQueryWrapper<ProductTrace>()
                .eq(ProductTrace::getProductId, traceSaveDTO.getProductId())
                .last("limit 1"));
        if (trace == null) {
            trace = new ProductTrace();
            trace.setProductId(traceSaveDTO.getProductId());
            fillTrace(trace, traceSaveDTO);
            productTraceMapper.insert(trace);
        } else {
            fillTrace(trace, traceSaveDTO);
            productTraceMapper.updateById(trace);
        }
        return buildTraceDetail(product);
    }

    @Override
    @Transactional
    public TraceListVO disable(Long id) {
        ProductTrace trace = productTraceMapper.selectById(id);
        if (trace == null) {
            throw new BusinessException("追溯信息不存在");
        }
        Product product = validateProduct(trace.getProductId());
        CurrentUserUtil.requireAdminOrOwner(product.getFarmerId(), "无权操作该追溯信息");
        trace.setTraceStatus(0);
        productTraceMapper.updateById(trace);
        return buildTraceDetail(product);
    }

    private Product validateProduct(Long productId) {
        if (productId == null) {
            throw new BusinessException("商品ID不能为空");
        }
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        CurrentUserUtil.requireAdminOrOwner(product.getFarmerId(), "无权访问该商品追溯信息");
        return product;
    }

    private void fillTrace(ProductTrace trace, TraceSaveDTO dto) {
        trace.setProductionDate(dto.getProductionDate());
        trace.setOriginDesc(dto.getOriginDesc());
        trace.setInspectDesc(dto.getInspectDesc());
        trace.setTraceStatus(dto.getTraceStatus() == null ? 1 : dto.getTraceStatus());
    }

    private List<TraceListVO> buildTraceList(List<Product> products, Integer traceStatus) {
        if (products == null || products.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> productIds = products.stream().map(Product::getId).collect(Collectors.toSet());
        Set<Long> farmerIds = products.stream().map(Product::getFarmerId).filter(id -> id != null).collect(Collectors.toSet());
        Set<Long> categoryIds = products.stream().map(Product::getCategoryId).filter(id -> id != null).collect(Collectors.toSet());

        Map<Long, ProductTrace> traceMap = productTraceMapper.selectList(new LambdaQueryWrapper<ProductTrace>().in(ProductTrace::getProductId, productIds)).stream()
                .collect(Collectors.toMap(ProductTrace::getProductId, trace -> trace, (current, ignored) -> current));
        Map<Long, String> farmerNameMap = farmerIds.isEmpty() ? Collections.emptyMap()
                : farmerMapper.selectBatchIds(farmerIds).stream().collect(Collectors.toMap(Farmer::getId, Farmer::getFarmerName));
        Map<Long, String> categoryNameMap = categoryIds.isEmpty() ? Collections.emptyMap()
                : productCategoryMapper.selectBatchIds(categoryIds).stream().collect(Collectors.toMap(ProductCategory::getId, ProductCategory::getCategoryName));

        List<TraceListVO> records = new ArrayList<>();
        for (Product product : products) {
            ProductTrace trace = traceMap.get(product.getId());
            if (traceStatus != null) {
                if (traceStatus == -1 && trace != null) {
                    continue;
                }
                if (traceStatus != -1 && (trace == null || !traceStatus.equals(trace.getTraceStatus()))) {
                    continue;
                }
            }
            TraceListVO vo = new TraceListVO();
            vo.setTraceId(trace == null ? null : trace.getId());
            vo.setProductId(product.getId());
            vo.setFarmerId(product.getFarmerId());
            vo.setCategoryId(product.getCategoryId());
            vo.setProductName(product.getProductName());
            vo.setFarmerName(farmerNameMap.getOrDefault(product.getFarmerId(), "-"));
            vo.setCategoryName(categoryNameMap.getOrDefault(product.getCategoryId(), "-"));
            vo.setOriginPlace(product.getOriginPlace());
            vo.setCoverImage(UrlUtils.toFullUrl(product.getCoverImage()));
            vo.setStock(product.getStock());
            vo.setSaleStatus(product.getSaleStatus());
            vo.setProductionDate(trace == null ? null : trace.getProductionDate());
            vo.setOriginDesc(trace == null ? null : trace.getOriginDesc());
            vo.setInspectDesc(trace == null ? null : trace.getInspectDesc());
            vo.setTraceStatus(trace == null ? null : trace.getTraceStatus());
            vo.setTraceMaintained(trace != null);
            records.add(vo);
        }
        return records;
    }

    private TraceListVO buildTraceDetail(Product product) {
        ProductTrace trace = productTraceMapper.selectOne(new LambdaQueryWrapper<ProductTrace>()
                .eq(ProductTrace::getProductId, product.getId())
                .last("limit 1"));
        String farmerName = "-";
        String categoryName = "-";

        if (product.getFarmerId() != null) {
            Farmer farmer = farmerMapper.selectById(product.getFarmerId());
            if (farmer != null) {
                farmerName = farmer.getFarmerName();
            }
        }
        if (product.getCategoryId() != null) {
            ProductCategory category = productCategoryMapper.selectById(product.getCategoryId());
            if (category != null) {
                categoryName = category.getCategoryName();
            }
        }

        TraceListVO vo = new TraceListVO();
        vo.setTraceId(trace == null ? null : trace.getId());
        vo.setProductId(product.getId());
        vo.setFarmerId(product.getFarmerId());
        vo.setCategoryId(product.getCategoryId());
        vo.setProductName(product.getProductName());
        vo.setFarmerName(farmerName);
        vo.setCategoryName(categoryName);
        vo.setOriginPlace(product.getOriginPlace());
        vo.setCoverImage(UrlUtils.toFullUrl(product.getCoverImage()));
        vo.setStock(product.getStock());
        vo.setSaleStatus(product.getSaleStatus());
        vo.setProductionDate(trace == null ? null : trace.getProductionDate());
        vo.setOriginDesc(trace == null ? null : trace.getOriginDesc());
        vo.setInspectDesc(trace == null ? null : trace.getInspectDesc());
        vo.setTraceStatus(trace == null ? null : trace.getTraceStatus());
        vo.setTraceMaintained(trace != null);
        return vo;
    }
}