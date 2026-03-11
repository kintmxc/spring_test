package com.example.spring_test.controller;

import com.example.spring_test.common.PageResult;
import com.example.spring_test.common.Result;
import com.example.spring_test.dto.FarmerAuditDTO;
import com.example.spring_test.dto.FarmerQueryDTO;
import com.example.spring_test.dto.FarmerSaveDTO;
import com.example.spring_test.service.FarmerService;
import com.example.spring_test.vo.FarmerDetailVO;
import com.example.spring_test.vo.FarmerListVO;
import com.example.spring_test.vo.OptionVO;
import com.example.spring_test.entity.Farmer;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/farmers")
public class FarmerController {
    private final FarmerService farmerService;

    public FarmerController(FarmerService farmerService) {
        this.farmerService = farmerService;
    }

    @GetMapping
    public Result<PageResult<FarmerListVO>> page(FarmerQueryDTO farmerQueryDTO) {
        return Result.success(farmerService.page(farmerQueryDTO));
    }

    @GetMapping("/approved")
    public Result<List<Farmer>> listApproved() {
        return Result.success(farmerService.listApproved());
    }

    @GetMapping("/options")
    public Result<List<OptionVO>> options() {
        return Result.success(farmerService.options());
    }

    @GetMapping("/{id}")
    public Result<FarmerDetailVO> detail(@PathVariable Long id) {
        return Result.success(farmerService.detail(id));
    }

    @PostMapping
    public Result<FarmerDetailVO> save(@RequestBody FarmerSaveDTO farmerSaveDTO) {
        return Result.success("新增农户成功", farmerService.save(farmerSaveDTO));
    }

    @PutMapping("/{id}")
    public Result<FarmerDetailVO> update(@PathVariable Long id, @RequestBody FarmerSaveDTO farmerSaveDTO) {
        return Result.success("更新农户成功", farmerService.update(id, farmerSaveDTO));
    }

    @PutMapping("/{id}/audit")
    public Result<FarmerDetailVO> audit(@PathVariable Long id, @RequestBody FarmerAuditDTO farmerAuditDTO) {
        return Result.success("审核农户成功", farmerService.audit(id, farmerAuditDTO));
    }

    @PutMapping("/{id}/status")
    public Result<FarmerDetailVO> updateStatus(@PathVariable Long id, @RequestParam Integer accountStatus) {
        return Result.success("更新农户状态成功", farmerService.updateStatus(id, accountStatus));
    }
}