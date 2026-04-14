package com.example.spring_test.service;

import com.example.spring_test.common.PageResult;
import com.example.spring_test.dto.FarmerAuditDTO;
import com.example.spring_test.dto.FarmerQueryDTO;
import com.example.spring_test.dto.FarmerSaveDTO;
import com.example.spring_test.entity.Farmer;
import com.example.spring_test.vo.FarmerDetailVO;
import com.example.spring_test.vo.FarmerListVO;
import com.example.spring_test.vo.OptionVO;
import java.util.List;

public interface FarmerService {
    PageResult<FarmerListVO> page(FarmerQueryDTO farmerQueryDTO);

    FarmerDetailVO detail(Long id);

    FarmerDetailVO save(FarmerSaveDTO farmerSaveDTO);

    FarmerDetailVO update(Long id, FarmerSaveDTO farmerSaveDTO);

    FarmerDetailVO audit(Long id, FarmerAuditDTO farmerAuditDTO);

    FarmerDetailVO updateStatus(Long id, Integer accountStatus);

    List<Farmer> listApproved();

    List<OptionVO> options();

    void delete(Long id);

    // --- For internal decoupling ---
    com.example.spring_test.entity.Farmer getById(Long id);
    java.util.Map<Long, String> getFarmerNamesByIds(java.util.Set<Long> ids);
    java.util.List<Long> getFarmerIdsByKeyword(String keyword);
}