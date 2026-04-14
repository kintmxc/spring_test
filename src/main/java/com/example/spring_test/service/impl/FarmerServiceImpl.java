package com.example.spring_test.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.spring_test.common.PageResult;
import com.example.spring_test.dto.FarmerAuditDTO;
import com.example.spring_test.dto.FarmerQueryDTO;
import com.example.spring_test.dto.FarmerSaveDTO;
import com.example.spring_test.entity.Farmer;
import com.example.spring_test.entity.Orders;
import com.example.spring_test.entity.Product;
import com.example.spring_test.enums.ProductSaleStatusEnum;
import com.example.spring_test.exception.BusinessException;
import com.example.spring_test.mapper.FarmerMapper;
import com.example.spring_test.mapper.OrdersMapper;
import com.example.spring_test.mapper.ProductMapper;
import com.example.spring_test.security.CurrentUserUtil;
import com.example.spring_test.service.FarmerService;
import com.example.spring_test.util.PasswordUtil;
import com.example.spring_test.vo.FarmerDetailVO;
import com.example.spring_test.vo.FarmerListVO;
import com.example.spring_test.vo.OptionVO;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class FarmerServiceImpl implements FarmerService {
    private final FarmerMapper farmerMapper;
    private final ProductMapper productMapper;
    private final OrdersMapper ordersMapper;

    public FarmerServiceImpl(FarmerMapper farmerMapper, ProductMapper productMapper, OrdersMapper ordersMapper) {
        this.farmerMapper = farmerMapper;
        this.productMapper = productMapper;
        this.ordersMapper = ordersMapper;
    }

    @Override
    public PageResult<FarmerListVO> page(FarmerQueryDTO farmerQueryDTO) {
        CurrentUserUtil.requireAdmin("仅管理员可查看农户管理列表");
        Page<Farmer> page = new Page<>(farmerQueryDTO.getPageNum(), farmerQueryDTO.getPageSize());
        LambdaQueryWrapper<Farmer> queryWrapper = new LambdaQueryWrapper<Farmer>()
                .like(farmerQueryDTO.getFarmerName() != null && !farmerQueryDTO.getFarmerName().isBlank(), Farmer::getFarmerName, farmerQueryDTO.getFarmerName())
                .eq(farmerQueryDTO.getAuthStatus() != null, Farmer::getAuthStatus, farmerQueryDTO.getAuthStatus())
                .eq(farmerQueryDTO.getAccountStatus() != null, Farmer::getAccountStatus, farmerQueryDTO.getAccountStatus())
                .orderByDesc(Farmer::getId);
        Page<Farmer> result = farmerMapper.selectPage(page, queryWrapper);
        return new PageResult<>(result.getTotal(), result.getCurrent(), result.getSize(), buildFarmerList(result.getRecords()));
    }

    @Override
    public FarmerDetailVO detail(Long id) {
        CurrentUserUtil.requireAdmin("仅管理员可查看农户详情");
        Farmer farmer = getFarmer(id);
        return buildFarmerDetail(farmer);
    }

    @Override
    public FarmerDetailVO save(FarmerSaveDTO farmerSaveDTO) {
        CurrentUserUtil.requireAdmin("仅管理员可新增农户");
        ensureLoginNameUnique(farmerSaveDTO.getLoginName(), null);
        Farmer farmer = new Farmer();
        farmer.setLoginName(farmerSaveDTO.getLoginName());
        farmer.setPassword(PasswordUtil.encode(farmerSaveDTO.getPassword()));
        farmer.setFarmerName(farmerSaveDTO.getFarmerName());
        farmer.setContactPhone(farmerSaveDTO.getContactPhone());
        farmer.setOriginPlace(farmerSaveDTO.getOriginPlace());
        farmer.setIdCardNo(farmerSaveDTO.getIdCardNo());
        farmer.setLicenseNo(farmerSaveDTO.getLicenseNo());
        farmer.setRemark(farmerSaveDTO.getRemark());
        farmer.setAuthStatus(0);
        farmer.setAccountStatus(1);
        farmerMapper.insert(farmer);
        return buildFarmerDetail(farmer);
    }

    @Override
    public FarmerDetailVO update(Long id, FarmerSaveDTO farmerSaveDTO) {
        CurrentUserUtil.requireAdmin("仅管理员可编辑农户");
        Farmer farmer = getFarmer(id);
        ensureLoginNameUnique(farmerSaveDTO.getLoginName(), id);
        farmer.setLoginName(farmerSaveDTO.getLoginName());
        if (farmerSaveDTO.getPassword() != null && !farmerSaveDTO.getPassword().isBlank()) {
            farmer.setPassword(PasswordUtil.encode(farmerSaveDTO.getPassword()));
        }
        farmer.setFarmerName(farmerSaveDTO.getFarmerName());
        farmer.setContactPhone(farmerSaveDTO.getContactPhone());
        farmer.setOriginPlace(farmerSaveDTO.getOriginPlace());
        farmer.setIdCardNo(farmerSaveDTO.getIdCardNo());
        farmer.setLicenseNo(farmerSaveDTO.getLicenseNo());
        farmer.setRemark(farmerSaveDTO.getRemark());
        farmerMapper.updateById(farmer);
        return buildFarmerDetail(farmer);
    }

    @Override
    public FarmerDetailVO audit(Long id, FarmerAuditDTO farmerAuditDTO) {
        CurrentUserUtil.requireAdmin("仅管理员可审核农户");
        Farmer farmer = getFarmer(id);
        farmer.setAuthStatus(farmerAuditDTO.getAuthStatus());
        farmer.setRemark(farmerAuditDTO.getRemark());
        farmerMapper.updateById(farmer);
        return buildFarmerDetail(farmer);
    }

    @Override
    public FarmerDetailVO updateStatus(Long id, Integer accountStatus) {
        CurrentUserUtil.requireAdmin("仅管理员可更新农户状态");
        Farmer farmer = getFarmer(id);
        farmer.setAccountStatus(accountStatus);
        farmerMapper.updateById(farmer);
        return buildFarmerDetail(farmer);
    }

    @Override
    public List<Farmer> listApproved() {
        CurrentUserUtil.requireAdmin("仅管理员可查看已审核农户列表");
        return farmerMapper.selectList(new LambdaQueryWrapper<Farmer>()
                .eq(Farmer::getAuthStatus, 1)
                .eq(Farmer::getAccountStatus, 1)
                .orderByDesc(Farmer::getId));
    }

    @Override
    public List<OptionVO> options() {
        if (CurrentUserUtil.isFarmer()) {
            Farmer farmer = getFarmer(CurrentUserUtil.currentUserId());
            return List.of(new OptionVO(farmer.getId(), farmer.getFarmerName()));
        }
        return listApproved().stream()
                .map(farmer -> new OptionVO(farmer.getId(), farmer.getFarmerName()))
                .toList();
    }

    private void ensureLoginNameUnique(String loginName, Long excludeId) {
        if (loginName == null || loginName.isBlank()) {
            throw new BusinessException("农户登录名不能为空");
        }
        Farmer existed = farmerMapper.selectOne(new LambdaQueryWrapper<Farmer>()
                .eq(Farmer::getLoginName, loginName)
                .ne(excludeId != null, Farmer::getId, excludeId)
                .last("limit 1"));
        if (existed != null) {
            throw new BusinessException("农户登录名已存在");
        }
    }

    private Farmer getFarmer(Long id) {
        Farmer farmer = farmerMapper.selectById(id);
        if (farmer == null) {
            throw new BusinessException("农户不存在");
        }
        return farmer;
    }

    private List<FarmerListVO> buildFarmerList(List<Farmer> farmers) {
        if (farmers == null || farmers.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> farmerIds = farmers.stream().map(Farmer::getId).collect(Collectors.toSet());
        List<Product> products = productMapper.selectList(new LambdaQueryWrapper<Product>().in(Product::getFarmerId, farmerIds));
        List<Orders> orders = ordersMapper.selectList(new LambdaQueryWrapper<Orders>().in(Orders::getFarmerId, farmerIds));

        Map<Long, Long> productCountMap = products.stream().collect(Collectors.groupingBy(Product::getFarmerId, Collectors.counting()));
        Map<Long, Long> onSaleProductCountMap = products.stream()
                .filter(product -> product.getSaleStatus() != null && product.getSaleStatus().equals(ProductSaleStatusEnum.ON_SHELF.getCode()))
                .collect(Collectors.groupingBy(Product::getFarmerId, Collectors.counting()));
        Map<Long, Long> orderCountMap = orders.stream().collect(Collectors.groupingBy(Orders::getFarmerId, Collectors.counting()));

        List<FarmerListVO> records = new ArrayList<>();
        for (Farmer farmer : farmers) {
            FarmerListVO vo = new FarmerListVO();
            vo.setId(farmer.getId());
            vo.setLoginName(farmer.getLoginName());
            vo.setFarmerName(farmer.getFarmerName());
            vo.setContactPhone(farmer.getContactPhone());
            vo.setOriginPlace(farmer.getOriginPlace());
            vo.setIdCardNo(farmer.getIdCardNo());
            vo.setLicenseNo(farmer.getLicenseNo());
            vo.setAuthStatus(farmer.getAuthStatus());
            vo.setAccountStatus(farmer.getAccountStatus());
            vo.setRemark(farmer.getRemark());
            vo.setCreateTime(farmer.getCreateTime());
            vo.setUpdateTime(farmer.getUpdateTime());
            vo.setAuthStatusText(authStatusText(farmer.getAuthStatus()));
            vo.setAccountStatusText(farmer.getAccountStatus() != null && farmer.getAccountStatus() == 1 ? "启用" : "禁用");
            vo.setProductCount(productCountMap.getOrDefault(farmer.getId(), 0L).intValue());
            vo.setOnSaleProductCount(onSaleProductCountMap.getOrDefault(farmer.getId(), 0L).intValue());
            vo.setOrderCount(orderCountMap.getOrDefault(farmer.getId(), 0L).intValue());
            records.add(vo);
        }
        return records;
    }

    private String authStatusText(Integer authStatus) {
        if (authStatus == null) {
            return "未知";
        }
        return switch (authStatus) {
            case 0 -> "待审核";
            case 1 -> "已通过";
            case 2 -> "已驳回";
            default -> "未知";
        };
    }

    private FarmerDetailVO buildFarmerDetail(Farmer farmer) {
        Long farmerId = farmer.getId();
        int productCount = productMapper.selectCount(new LambdaQueryWrapper<Product>().eq(Product::getFarmerId, farmerId)).intValue();
        int onSaleProductCount = productMapper.selectCount(new LambdaQueryWrapper<Product>()
                .eq(Product::getFarmerId, farmerId)
                .eq(Product::getSaleStatus, ProductSaleStatusEnum.ON_SHELF.getCode())).intValue();
        int orderCount = ordersMapper.selectCount(new LambdaQueryWrapper<Orders>().eq(Orders::getFarmerId, farmerId)).intValue();

        FarmerDetailVO vo = new FarmerDetailVO();
        vo.setId(farmer.getId());
        vo.setLoginName(farmer.getLoginName());
        vo.setFarmerName(farmer.getFarmerName());
        vo.setContactPhone(farmer.getContactPhone());
        vo.setOriginPlace(farmer.getOriginPlace());
        vo.setIdCardNo(farmer.getIdCardNo());
        vo.setLicenseNo(farmer.getLicenseNo());
        vo.setAuthStatus(farmer.getAuthStatus());
        vo.setAuthStatusText(authStatusText(farmer.getAuthStatus()));
        vo.setAccountStatus(farmer.getAccountStatus());
        vo.setAccountStatusText(farmer.getAccountStatus() != null && farmer.getAccountStatus() == 1 ? "启用" : "禁用");
        vo.setRemark(farmer.getRemark());
        vo.setCreateTime(farmer.getCreateTime());
        vo.setUpdateTime(farmer.getUpdateTime());
        vo.setProductCount(productCount);
        vo.setOnSaleProductCount(onSaleProductCount);
        vo.setOrderCount(orderCount);
        return vo;
    }

    @Override
    public void delete(Long id) {
        Farmer farmer = farmerMapper.selectById(id);
        if (farmer == null) {
            throw new BusinessException("农户不存在");
        }
        farmerMapper.deleteById(id);
    }

    // --- 内部解耦方法 ---
    @Override
    public Farmer getById(Long id) {
        return farmerMapper.selectById(id);
    }

    @Override
    public Map<Long, String> getFarmerNamesByIds(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Farmer> farmers = farmerMapper.selectBatchIds(ids);
        return farmers.stream().collect(Collectors.toMap(Farmer::getId, Farmer::getFarmerName));
    }

    @Override
    public List<Long> getFarmerIdsByKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return Collections.emptyList();
        }
        return farmerMapper.selectList(new LambdaQueryWrapper<Farmer>()
                .like(Farmer::getFarmerName, keyword))
                .stream().map(Farmer::getId).collect(Collectors.toList());
    }
}