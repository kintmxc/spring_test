package com.example.spring_test.service;

import com.example.spring_test.dto.CategorySaveDTO;
import com.example.spring_test.vo.CategoryVO;
import com.example.spring_test.vo.OptionVO;
import java.util.List;

public interface CategoryService {
    List<CategoryVO> list();

    List<OptionVO> options();

    CategoryVO save(CategorySaveDTO categorySaveDTO);

    CategoryVO update(Long id, CategorySaveDTO categorySaveDTO);

    void delete(Long id);

    // --- For internal decoupling ---
    com.example.spring_test.entity.ProductCategory getById(Long id);
    java.util.Map<Long, String> getCategoryNamesByIds(java.util.Set<Long> ids);
    java.util.List<Long> getCategoryIdsByKeyword(String keyword);
}