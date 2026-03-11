package com.example.spring_test.common;

import java.util.List;
import lombok.Data;

@Data
public class PageResult<T> {
    private long total;
    private long pageNum;
    private long pageSize;
    private List<T> records;

    public PageResult(long total, long pageNum, long pageSize, List<T> records) {
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.records = records;
    }
}