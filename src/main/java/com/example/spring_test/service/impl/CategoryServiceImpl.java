package com.example.spring_test.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.spring_test.dto.CategorySaveDTO;
import com.example.spring_test.entity.Product;
import com.example.spring_test.entity.ProductCategory;
import com.example.spring_test.exception.BusinessException;
import com.example.spring_test.exception.ForbiddenException;
import com.example.spring_test.mapper.ProductCategoryMapper;
import com.example.spring_test.mapper.ProductMapper;
import com.example.spring_test.security.SessionUser;
import com.example.spring_test.security.SessionUserHolder;
import com.example.spring_test.service.CategoryService;
import com.example.spring_test.vo.CategoryVO;
import com.example.spring_test.vo.OptionVO;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class CategoryServiceImpl implements CategoryService {
    private final ProductCategoryMapper productCategoryMapper;
    private final ProductMapper productMapper;

    public CategoryServiceImpl(ProductCategoryMapper productCategoryMapper, ProductMapper productMapper) {
        this.productCategoryMapper = productCategoryMapper;
        this.productMapper = productMapper;
    }

    @Override
    @Cacheable(value = "categories", key = "'all'")
    public List<CategoryVO> list() {
        List<ProductCategory> categories = productCategoryMapper.selectList(new LambdaQueryWrapper<ProductCategory>()
                .orderByAsc(ProductCategory::getSortNo)
                .orderByAsc(ProductCategory::getId));
        return buildCategoryList(categories);
    }

    @Override
    @Cacheable(value = "categoryOptions", key = "'all'")
    public List<OptionVO> options() {
        return productCategoryMapper.selectList(new LambdaQueryWrapper<ProductCategory>()
                        .eq(ProductCategory::getStatus, 1)
                        .orderByAsc(ProductCategory::getSortNo)
                        .orderByAsc(ProductCategory::getId))
                .stream()
                .map(category -> new OptionVO(category.getId(), category.getCategoryName()))
                .toList();
    }

    @Override
    @CacheEvict(value = {"categories", "categoryOptions"}, allEntries = true)
    public CategoryVO save(CategorySaveDTO categorySaveDTO) {
        requireAdmin("仅管理员可新增分类");
        ensureUnique(categorySaveDTO.getCategoryName(), null);
        
        // 自动生成排序值：最大排序值 + 1
        Integer sortNo = categorySaveDTO.getSortNo();
        if (sortNo == null || sortNo < 0) {
            // 查询当前最大排序值
            ProductCategory maxSortCategory = productCategoryMapper.selectOne(
                new LambdaQueryWrapper<ProductCategory>()
                    .orderByDesc(ProductCategory::getSortNo)
                    .last("limit 1")
            );
            if (maxSortCategory != null) {
                sortNo = maxSortCategory.getSortNo() + 1;
            } else {
                sortNo = 1; // 第一个分类从1开始
            }
        }
        
        ProductCategory category = new ProductCategory();
        category.setCategoryName(categorySaveDTO.getCategoryName());
        category.setSortNo(sortNo);
        category.setStatus(categorySaveDTO.getStatus() == null ? 1 : categorySaveDTO.getStatus());
        productCategoryMapper.insert(category);
        return toCategoryVO(category, 0);
    }

    @Override
    @CacheEvict(value = {"categories", "categoryOptions"}, allEntries = true)
    public CategoryVO update(Long id, CategorySaveDTO categorySaveDTO) {
        requireAdmin("仅管理员可编辑分类");
        ProductCategory category = getById(id);
        ensureUnique(categorySaveDTO.getCategoryName(), id);
        category.setCategoryName(categorySaveDTO.getCategoryName());
        category.setSortNo(categorySaveDTO.getSortNo() == null ? category.getSortNo() : categorySaveDTO.getSortNo());
        category.setStatus(categorySaveDTO.getStatus() == null ? category.getStatus() : categorySaveDTO.getStatus());
        productCategoryMapper.updateById(category);
        int productCount = productMapper.selectCount(new LambdaQueryWrapper<Product>()
                .eq(Product::getCategoryId, category.getId())).intValue();
        return toCategoryVO(category, productCount);
    }

    @Override
    @CacheEvict(value = {"categories", "categoryOptions"}, allEntries = true)
    public void delete(Long id) {
        requireAdmin("仅管理员可删除分类");
        ProductCategory category = getById(id);
        Long count = productMapper.selectCount(new LambdaQueryWrapper<Product>()
                .eq(Product::getCategoryId, category.getId()));
        if (count != null && count > 0) {
            throw new BusinessException("分类下存在商品，不能删除");
        }
        productCategoryMapper.deleteById(id);
    }

    @Override
    public ProductCategory getById(Long id) {
        return productCategoryMapper.selectById(id);
    }

    @Override
    public Map<Long, String> getCategoryNamesByIds(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<ProductCategory> categories = productCategoryMapper.selectBatchIds(ids);
        return categories.stream().collect(Collectors.toMap(ProductCategory::getId, ProductCategory::getCategoryName));
    }

    @Override
    public List<Long> getCategoryIdsByKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return Collections.emptyList();
        }
        return productCategoryMapper.selectList(new LambdaQueryWrapper<ProductCategory>()
                .like(ProductCategory::getCategoryName, keyword))
                .stream().map(ProductCategory::getId).collect(Collectors.toList());
    }

    private void ensureUnique(String categoryName, Long excludeId) {
        if (categoryName == null || categoryName.isBlank()) {
            throw new BusinessException("分类名称不能为空");
        }
        ProductCategory existed = productCategoryMapper.selectOne(new LambdaQueryWrapper<ProductCategory>()
                .eq(ProductCategory::getCategoryName, categoryName)
                .ne(excludeId != null, ProductCategory::getId, excludeId)
                .last("limit 1"));
        if (existed != null) {
            throw new BusinessException("分类名称已存在");
        }
    }

    private List<CategoryVO> buildCategoryList(List<ProductCategory> categories) {
        if (categories == null || categories.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> categoryIds = categories.stream().map(ProductCategory::getId).collect(Collectors.toSet());
        Map<Long, Long> productCountMap = productMapper.selectList(new LambdaQueryWrapper<Product>()
                        .in(Product::getCategoryId, categoryIds))
                .stream()
                .collect(Collectors.groupingBy(Product::getCategoryId, Collectors.counting()));
        return categories.stream()
                .map(category -> toCategoryVO(category, productCountMap.getOrDefault(category.getId(), 0L).intValue()))
                .toList();
    }

    private CategoryVO toCategoryVO(ProductCategory category, int productCount) {
        CategoryVO vo = new CategoryVO();
        vo.setId(category.getId());
        vo.setCategoryName(category.getCategoryName());
        vo.setSortNo(category.getSortNo());
        vo.setStatus(category.getStatus());
        vo.setStatusText(category.getStatus() != null && category.getStatus() == 1 ? "启用" : "停用");
        vo.setProductCount(productCount);
        vo.setCreateTime(category.getCreateTime());
        vo.setUpdateTime(category.getUpdateTime());
        return vo;
    }

    private void requireAdmin(String message) {
        SessionUser currentUser = SessionUserHolder.get();
        if (currentUser == null || !"ADMIN".equalsIgnoreCase(currentUser.getRoleCode())) {
            throw new ForbiddenException(message);
        }
    }
}