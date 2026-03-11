package com.example.spring_test.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.spring_test.dto.CategorySaveDTO;
import com.example.spring_test.entity.Product;
import com.example.spring_test.entity.ProductCategory;
import com.example.spring_test.exception.BusinessException;
import com.example.spring_test.mapper.ProductCategoryMapper;
import com.example.spring_test.mapper.ProductMapper;
import com.example.spring_test.security.CurrentUserUtil;
import com.example.spring_test.service.CategoryService;
import com.example.spring_test.vo.CategoryVO;
import com.example.spring_test.vo.OptionVO;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
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
    public List<CategoryVO> list() {
        CurrentUserUtil.requireAdmin("仅管理员可查看分类管理列表");
        List<ProductCategory> categories = productCategoryMapper.selectList(new LambdaQueryWrapper<ProductCategory>()
                .orderByAsc(ProductCategory::getSortNo)
                .orderByAsc(ProductCategory::getId));
        return buildCategoryList(categories);
    }

    @Override
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
    public CategoryVO save(CategorySaveDTO categorySaveDTO) {
        CurrentUserUtil.requireAdmin("仅管理员可新增分类");
        ensureUnique(categorySaveDTO.getCategoryName(), null);
        ProductCategory category = new ProductCategory();
        category.setCategoryName(categorySaveDTO.getCategoryName());
        category.setSortNo(categorySaveDTO.getSortNo() == null ? 0 : categorySaveDTO.getSortNo());
        category.setStatus(categorySaveDTO.getStatus() == null ? 1 : categorySaveDTO.getStatus());
        productCategoryMapper.insert(category);
        return toCategoryVO(category, 0);
    }

    @Override
    public CategoryVO update(Long id, CategorySaveDTO categorySaveDTO) {
        CurrentUserUtil.requireAdmin("仅管理员可编辑分类");
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
    public void delete(Long id) {
        CurrentUserUtil.requireAdmin("仅管理员可删除分类");
        ProductCategory category = getById(id);
        Long count = productMapper.selectCount(new LambdaQueryWrapper<Product>()
                .eq(Product::getCategoryId, category.getId()));
        if (count != null && count > 0) {
            throw new BusinessException("分类下存在商品，不能删除");
        }
        productCategoryMapper.deleteById(id);
    }

    private ProductCategory getById(Long id) {
        ProductCategory category = productCategoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException("分类不存在");
        }
        return category;
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
}