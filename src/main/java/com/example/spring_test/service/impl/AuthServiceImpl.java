package com.example.spring_test.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.spring_test.dto.LoginDTO;
import com.example.spring_test.entity.Admin;
import com.example.spring_test.entity.Farmer;
import com.example.spring_test.enums.RoleEnum;
import com.example.spring_test.exception.BusinessException;
import com.example.spring_test.mapper.AdminMapper;
import com.example.spring_test.mapper.FarmerMapper;
import com.example.spring_test.service.AuthService;
import com.example.spring_test.util.PasswordUtil;
import com.example.spring_test.vo.LoginVO;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {
    private final AdminMapper adminMapper;
    private final FarmerMapper farmerMapper;

    public AuthServiceImpl(AdminMapper adminMapper, FarmerMapper farmerMapper) {
        this.adminMapper = adminMapper;
        this.farmerMapper = farmerMapper;
    }

    @Override
    public LoginVO login(LoginDTO loginDTO) {
        Admin admin = adminMapper.selectOne(new LambdaQueryWrapper<Admin>()
                .eq(Admin::getUsername, loginDTO.getUsername())
                .last("limit 1"));
        if (admin != null) {
            return loginAdmin(admin, loginDTO.getPassword());
        }
        Farmer farmer = farmerMapper.selectOne(new LambdaQueryWrapper<Farmer>()
                .eq(Farmer::getLoginName, loginDTO.getUsername())
                .last("limit 1"));
        if (farmer != null) {
            return loginFarmer(farmer, loginDTO.getPassword());
        }
        throw new BusinessException("用户名或密码错误");
    }

    private LoginVO loginAdmin(Admin admin, String rawPassword) {
        if (!PasswordUtil.matches(rawPassword, admin.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        if (admin.getStatus() != null && admin.getStatus() == 0) {
            throw new BusinessException("当前账号已被禁用");
        }
        if (PasswordUtil.needsUpgrade(admin.getPassword())) {
            admin.setPassword(PasswordUtil.encode(rawPassword));
        }
        admin.setLastLoginTime(LocalDateTime.now());
        adminMapper.updateById(admin);
        return new LoginVO(admin.getId(), admin.getUsername(), admin.getRealName(), RoleEnum.ADMIN.name());
    }

    private LoginVO loginFarmer(Farmer farmer, String rawPassword) {
        if (!PasswordUtil.matches(rawPassword, farmer.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        if (farmer.getAccountStatus() != null && farmer.getAccountStatus() == 0) {
            throw new BusinessException("当前农户账号已被禁用");
        }
        if (farmer.getAuthStatus() == null || farmer.getAuthStatus() != 1) {
            throw new BusinessException("当前农户未通过审核，无法登录");
        }
        if (PasswordUtil.needsUpgrade(farmer.getPassword())) {
            farmer.setPassword(PasswordUtil.encode(rawPassword));
            farmerMapper.updateById(farmer);
        }
        return new LoginVO(farmer.getId(), farmer.getLoginName(), farmer.getFarmerName(), RoleEnum.FARMER.name());
    }
}