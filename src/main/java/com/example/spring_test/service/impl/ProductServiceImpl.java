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
import com.example.spring_test.entity.ProductImage;
import com.example.spring_test.entity.ProductTrace;
import com.example.spring_test.exception.BusinessException;
import com.example.spring_test.exception.ForbiddenException;
import com.example.spring_test.mapper.FarmerMapper;
import com.example.spring_test.mapper.OrderItemMapper;
import com.example.spring_test.mapper.ProductCategoryMapper;
import com.example.spring_test.mapper.ProductImageMapper;
import com.example.spring_test.mapper.ProductMapper;
import com.example.spring_test.mapper.ProductTraceMapper;
import com.example.spring_test.security.CurrentUserUtil;
import com.example.spring_test.security.SessionUser;
import com.example.spring_test.security.SessionUserHolder;
import com.example.spring_test.service.ProductService;
import com.example.spring_test.util.UrlUtils;
import com.example.spring_test.vo.ProductDetailVO;
import com.example.spring_test.vo.ProductListVO;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductServiceImpl implements ProductService {
    private final ProductMapper productMapper;
    private final ProductCategoryMapper productCategoryMapper;
    private final FarmerMapper farmerMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductTraceMapper productTraceMapper;
    private final ProductImageMapper productImageMapper;

    public ProductServiceImpl(ProductMapper productMapper,
                              ProductCategoryMapper productCategoryMapper,
                              FarmerMapper farmerMapper,
                              OrderItemMapper orderItemMapper,
                              ProductTraceMapper productTraceMapper,
                              ProductImageMapper productImageMapper) {
        this.productMapper = productMapper;
        this.productCategoryMapper = productCategoryMapper;
        this.farmerMapper = farmerMapper;
        this.orderItemMapper = orderItemMapper;
        this.productTraceMapper = productTraceMapper;
        this.productImageMapper = productImageMapper;
    }

    @Override
    public PageResult<ProductListVO> page(ProductQueryDTO productQueryDTO) {
        Page<Product> page = new Page<>(productQueryDTO.getPageNum(), productQueryDTO.getPageSize());
        Long farmerId = resolveFarmerId(productQueryDTO.getFarmerId());
        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<Product>()
                .like(productQueryDTO.getProductName() != null && !productQueryDTO.getProductName().isBlank(), Product::getProductName, productQueryDTO.getProductName())
                .like(productQueryDTO.getOriginPlace() != null && !productQueryDTO.getOriginPlace().isBlank(), Product::getOriginPlace, productQueryDTO.getOriginPlace())
                .eq(productQueryDTO.getCategoryId() != null, Product::getCategoryId, productQueryDTO.getCategoryId())
                .eq(farmerId != null, Product::getFarmerId, farmerId)
                .eq(productQueryDTO.getSaleStatus() != null, Product::getSaleStatus, productQueryDTO.getSaleStatus())
                .orderByDesc(Product::getId);
        if (shouldLimitToOnShelf(productQueryDTO.getSaleStatus())) {
            queryWrapper.eq(Product::getSaleStatus, 1);
        }
        applyStockFilter(queryWrapper, productQueryDTO.getStockStatus());
        if (productQueryDTO.getHasCoverImage() != null) {
            if (productQueryDTO.getHasCoverImage() == 1) {
                queryWrapper.isNotNull(Product::getCoverImage).ne(Product::getCoverImage, "");
            } else if (productQueryDTO.getHasCoverImage() == 0) {
                queryWrapper.and(wrapper -> wrapper.isNull(Product::getCoverImage).or().eq(Product::getCoverImage, ""));
            }
        }
        if (productQueryDTO.getKeyword() != null && !productQueryDTO.getKeyword().isBlank()) {
            String keyword = productQueryDTO.getKeyword().trim();
            List<Long> matchedFarmerIds = findFarmerIdsByKeyword(keyword);
            List<Long> matchedCategoryIds = findCategoryIdsByKeyword(keyword);
            queryWrapper.and(wrapper -> {
                wrapper.like(Product::getProductName, keyword);
                if (!matchedFarmerIds.isEmpty()) {
                    wrapper.or().in(Product::getFarmerId, matchedFarmerIds);
                }
                if (!matchedCategoryIds.isEmpty()) {
                    wrapper.or().in(Product::getCategoryId, matchedCategoryIds);
                }
            });
        }
        Page<Product> result = productMapper.selectPage(page, queryWrapper);
        List<ProductListVO> records = buildProductList(result.getRecords());
        return new PageResult<>(result.getTotal(), result.getCurrent(), result.getSize(), records);
    }

    @Override
    @Cacheable(value = "productDetail", key = "#id", unless = "#result == null")
    public ProductDetailVO detail(Long id) {
        Product product = getReadableProduct(id);
        return buildProductDetail(product);
    }

    @Override
    @CacheEvict(value = "productDetail", key = "#result.id")
    public ProductDetailVO save(ProductSaveDTO productSaveDTO) {
        Long farmerId = resolveWritableFarmerId(productSaveDTO.getFarmerId());
        validateReferences(productSaveDTO.getCategoryId(), farmerId);
        Product product = new Product();
        productSaveDTO.setFarmerId(farmerId);
        fillProduct(product, productSaveDTO);
        product.setSalesCount(0);
        productMapper.insert(product);
        saveOrUpdateTrace(product.getId(), productSaveDTO);
        saveOrUpdateImages(product.getId(), productSaveDTO.getImages());
        return buildProductDetail(product);
    }

    @Override
    @CacheEvict(value = "productDetail", key = "#id")
    public ProductDetailVO update(Long id, ProductSaveDTO productSaveDTO) {
        Product product = getProduct(id);
        Long farmerId = resolveWritableFarmerId(productSaveDTO.getFarmerId());
        validateReferences(productSaveDTO.getCategoryId(), farmerId);
        productSaveDTO.setFarmerId(farmerId);
        fillProduct(product, productSaveDTO);
        productMapper.updateById(product);
        saveOrUpdateTrace(id, productSaveDTO);
        saveOrUpdateImages(id, productSaveDTO.getImages());
        return buildProductDetail(product);
    }

    private void saveOrUpdateTrace(Long productId, ProductSaveDTO dto) {
        if (dto.getProductionDate() == null && dto.getInspectDesc() == null) {
            return;
        }
        ProductTrace trace = productTraceMapper.selectOne(new LambdaQueryWrapper<ProductTrace>()
                .eq(ProductTrace::getProductId, productId)
                .last("limit 1"));
        if (trace == null) {
            trace = new ProductTrace();
            trace.setProductId(productId);
            trace.setTraceStatus(1);
        }
        if (dto.getProductionDate() != null) {
            trace.setProductionDate(dto.getProductionDate());
        }
        if (dto.getInspectDesc() != null) {
            trace.setInspectDesc(dto.getInspectDesc());
        }
        if (trace.getId() == null) {
            productTraceMapper.insert(trace);
        } else {
            productTraceMapper.updateById(trace);
        }
    }
    
    private void saveOrUpdateImages(Long productId, List<String> images) {
        if (images == null || images.isEmpty()) {
            return;
        }
        
        productImageMapper.delete(new LambdaQueryWrapper<ProductImage>()
            .eq(ProductImage::getProductId, productId));
        
        for (int i = 0; i < images.size(); i++) {
            String imageUrl = images.get(i);
            if (imageUrl != null && !imageUrl.isBlank()) {
                ProductImage productImage = new ProductImage();
                productImage.setProductId(productId);
                productImage.setImageUrl(imageUrl);
                productImage.setSortOrder(i);
                productImageMapper.insert(productImage);
            }
        }
    }

    @Override
    @CacheEvict(value = "productDetail", key = "#id")
    public ProductDetailVO updateSaleStatus(Long id, Integer saleStatus) {
        Product product = getProduct(id);
        product.setSaleStatus(saleStatus);
        productMapper.updateById(product);
        return buildSimpleProductDetail(product);
    }

    @Override
    @CacheEvict(value = "productDetail", key = "#id")
    public ProductDetailVO updateStock(Long id, ProductStockUpdateDTO productStockUpdateDTO) {
        Product product = getProduct(id);
        if (productStockUpdateDTO.getStock() == null || productStockUpdateDTO.getStock() < 0) {
            throw new BusinessException("库存必须为大于等于0的整数");
        }
        product.setStock(productStockUpdateDTO.getStock());
        productMapper.updateById(product);
        return buildSimpleProductDetail(product);
    }

    private ProductDetailVO buildSimpleProductDetail(Product product) {
        ProductDetailVO vo = new ProductDetailVO();
        vo.setId(product.getId());
        vo.setFarmerId(product.getFarmerId());
        vo.setCategoryId(product.getCategoryId());
        vo.setProductName(product.getProductName());
        vo.setPrice(product.getPrice());
        vo.setStock(product.getStock());
        vo.setUnitName(product.getUnitName());
        vo.setOriginPlace(product.getOriginPlace());
        vo.setCoverImage(UrlUtils.toFullUrl(product.getCoverImage()));
        
        List<ProductImage> productImages = productImageMapper.selectList(
            new LambdaQueryWrapper<ProductImage>()
                .eq(ProductImage::getProductId, product.getId())
                .orderByAsc(ProductImage::getSortOrder)
        );
        if (productImages != null && !productImages.isEmpty()) {
            vo.setImages(productImages.stream()
                .map(img -> UrlUtils.toFullUrl(img.getImageUrl()))
                .collect(Collectors.toList()));
        }
        
        vo.setDescription(product.getDescription());
        vo.setSaleStatus(product.getSaleStatus());
        vo.setSalesCount(product.getSalesCount());
        vo.setCreateTime(product.getCreateTime());
        vo.setUpdateTime(product.getUpdateTime());
        return vo;
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
        vo.setCoverImage(UrlUtils.toFullUrl(product.getCoverImage()));
        
        List<ProductImage> productImages = productImageMapper.selectList(
            new LambdaQueryWrapper<ProductImage>()
                .eq(ProductImage::getProductId, product.getId())
                .orderByAsc(ProductImage::getSortOrder)
        );
        if (productImages != null && !productImages.isEmpty()) {
            vo.setImages(productImages.stream()
                .map(img -> UrlUtils.toFullUrl(img.getImageUrl()))
                .collect(Collectors.toList()));
        }
        
        vo.setDescription(product.getDescription());
        vo.setSaleStatus(product.getSaleStatus());
        vo.setSalesCount(product.getSalesCount());
        vo.setCreateTime(product.getCreateTime());
        vo.setUpdateTime(product.getUpdateTime());
        vo.setCategoryName(category == null ? "-" : category.getCategoryName());
        vo.setFarmerName(farmer == null ? "-" : farmer.getFarmerName());
        vo.setFarmerDesc(farmer == null ? "-" : farmer.getOriginPlace());
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
    @CacheEvict(value = "productDetail", key = "#id")
    public void delete(Long id) {
        Product product = getProduct(id);
        if (hasOrdersForProduct(product.getId())) {
            throw new BusinessException("该商品已有订单记录，无法删除");
        }
        
        productImageMapper.delete(new LambdaQueryWrapper<ProductImage>()
            .eq(ProductImage::getProductId, product.getId()));
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

    private Product getReadableProduct(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        SessionUser currentUser = SessionUserHolder.get();
        if (currentUser == null || isConsumer(currentUser)) {
            if (product.getSaleStatus() == null || product.getSaleStatus() != 1) {
                throw new BusinessException("商品不存在或已下架");
            }
            return product;
        }
        if (isAdmin(currentUser) || currentUser.getUserId().equals(product.getFarmerId())) {
            return product;
        }
        throw new ForbiddenException("无权访问该商品");
    }

    private Long resolveFarmerId(Long queryFarmerId) {
        SessionUser currentUser = SessionUserHolder.get();
        if (currentUser == null) {
            return queryFarmerId;
        }
        if (isFarmer(currentUser)) {
            return currentUser.getUserId();
        }
        return queryFarmerId;
    }

    private boolean shouldLimitToOnShelf(Integer requestedSaleStatus) {
        SessionUser currentUser = SessionUserHolder.get();
        if (currentUser == null) {
            return true;
        }
        if (isConsumer(currentUser)) {
            return true;
        }
        if (isFarmer(currentUser)) {
            return false;
        }
        return requestedSaleStatus == null;
    }

    private Long resolveWritableFarmerId(Long farmerId) {
        SessionUser currentUser = SessionUserHolder.get();
        if (isFarmer(currentUser)) {
            if (farmerId != null && !currentUser.getUserId().equals(farmerId)) {
                throw new ForbiddenException("农户只能新增或编辑自己的商品，不能指定其他农户");
            }
            return currentUser.getUserId();
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
            : loadCategoryNameMap(categoryIds);
        Map<Long, String> farmerNameMap = farmerIds.isEmpty()
                ? Collections.emptyMap()
            : loadFarmerNameMap(farmerIds);

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
        vo.setCoverImage(UrlUtils.toFullUrl(product.getCoverImage()));
        
        List<ProductImage> productImages = productImageMapper.selectList(
            new LambdaQueryWrapper<ProductImage>()
                .eq(ProductImage::getProductId, product.getId())
                .orderByAsc(ProductImage::getSortOrder)
        );
        if (productImages != null && !productImages.isEmpty()) {
            vo.setImages(productImages.stream()
                .map(img -> UrlUtils.toFullUrl(img.getImageUrl()))
                .collect(Collectors.toList()));
        }
        
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

    private boolean isAdmin(SessionUser user) {
        return user != null && "ADMIN".equalsIgnoreCase(user.getRoleCode());
    }

    private boolean isFarmer(SessionUser user) {
        return user != null && "FARMER".equalsIgnoreCase(user.getRoleCode());
    }

    private boolean isConsumer(SessionUser user) {
        return user != null && "CONSUMER".equalsIgnoreCase(user.getRoleCode());
    }

    private boolean hasOrdersForProduct(Long productId) {
        if (productId == null) {
            return false;
        }
        Long count = orderItemMapper.selectCount(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getProductId, productId));
        return count != null && count > 0;
    }

    private List<Long> findCategoryIdsByKeyword(String keyword) {
        return productCategoryMapper.selectList(new LambdaQueryWrapper<ProductCategory>()
                        .like(ProductCategory::getCategoryName, keyword))
                .stream()
                .map(ProductCategory::getId)
                .toList();
    }

    private List<Long> findFarmerIdsByKeyword(String keyword) {
        return farmerMapper.selectList(new LambdaQueryWrapper<Farmer>()
                        .like(Farmer::getFarmerName, keyword)
                        .or()
                        .like(Farmer::getLoginName, keyword)
                        .or()
                        .like(Farmer::getContactPhone, keyword))
                .stream()
                .map(Farmer::getId)
                .toList();
    }

    private Map<Long, String> loadCategoryNameMap(Set<Long> categoryIds) {
        return productCategoryMapper.selectBatchIds(categoryIds).stream()
                .collect(Collectors.toMap(ProductCategory::getId, ProductCategory::getCategoryName));
    }

    private Map<Long, String> loadFarmerNameMap(Set<Long> farmerIds) {
        return farmerMapper.selectBatchIds(farmerIds).stream()
                .collect(Collectors.toMap(Farmer::getId, Farmer::getFarmerName));
    }
}