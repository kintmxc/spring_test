package com.example.spring_test.service;

import com.example.spring_test.common.PageResult;
import com.example.spring_test.dto.TraceQueryDTO;
import com.example.spring_test.dto.TraceSaveDTO;
import com.example.spring_test.vo.TraceListVO;

public interface TraceService {
    PageResult<TraceListVO> page(TraceQueryDTO traceQueryDTO);

    TraceListVO getByProductId(Long productId);

    TraceListVO saveOrUpdate(TraceSaveDTO traceSaveDTO);

    TraceListVO disable(Long id);
}