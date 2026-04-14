package com.example.spring_test.common;

import java.util.List;
import lombok.Data;

@Data
public class PageResult<T> {
    private long total;
    private long pageNum;
    private long page;
    private long pageSize;
    private List<T> records;
    private List<T> list;
    private boolean hasMore;

    public PageResult(long total, long pageNum, long pageSize, List<T> records) {
        this.total = total;
        this.pageNum = pageNum;
        this.page = pageNum;
        this.pageSize = pageSize;
        this.records = records;
        this.list = records;
        this.hasMore = pageNum * pageSize < total;
    }
}