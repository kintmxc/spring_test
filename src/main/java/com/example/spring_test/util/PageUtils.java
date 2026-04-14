package com.example.spring_test.util;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.spring_test.common.PageResult;
import com.example.spring_test.dto.PageDTO;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class PageUtils {

    private PageUtils() {
    }

    public static <T> Page<T> createPage(PageDTO dto) {
        return new Page<>(dto.getPageNum(), dto.getPageSize());
    }

    public static <T> PageResult<T> toPageResult(Page<T> page) {
        return new PageResult<>(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords());
    }

    public static <T, R> PageResult<R> toPageResult(Page<T> page, Function<T, R> converter) {
        List<R> records = page.getRecords().stream()
                .map(converter)
                .collect(Collectors.toList());
        return new PageResult<>(page.getTotal(), page.getCurrent(), page.getSize(), records);
    }
}
