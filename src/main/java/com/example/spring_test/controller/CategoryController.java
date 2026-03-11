package com.example.spring_test.controller;

import com.example.spring_test.common.Result;
import com.example.spring_test.dto.CategorySaveDTO;
import com.example.spring_test.service.CategoryService;
import com.example.spring_test.vo.CategoryVO;
import com.example.spring_test.vo.OptionVO;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public Result<List<CategoryVO>> list() {
        return Result.success(categoryService.list());
    }

    @GetMapping("/options")
    public Result<List<OptionVO>> options() {
        return Result.success(categoryService.options());
    }

    @PostMapping
    public Result<CategoryVO> save(@RequestBody CategorySaveDTO categorySaveDTO) {
        return Result.success("新增分类成功", categoryService.save(categorySaveDTO));
    }

    @PutMapping("/{id}")
    public Result<CategoryVO> update(@PathVariable Long id, @RequestBody CategorySaveDTO categorySaveDTO) {
        return Result.success("更新分类成功", categoryService.update(id, categorySaveDTO));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return Result.success("删除分类成功", null);
    }
}