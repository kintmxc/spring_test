package com.example.spring_test.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.spring_test.config.SecurityProperties;
import com.example.spring_test.dto.LoginDTO;
import com.example.spring_test.entity.Admin;
import com.example.spring_test.entity.ConsumerUser;
import com.example.spring_test.entity.Farmer;
import com.example.spring_test.enums.RoleEnum;
import com.example.spring_test.exception.BusinessException;
import com.example.spring_test.mapper.AdminMapper;
import com.example.spring_test.mapper.ConsumerUserMapper;
import com.example.spring_test.mapper.FarmerMapper;
import com.example.spring_test.service.AuthService;
import com.example.spring_test.util.PasswordUtil;
import com.example.spring_test.vo.LoginVO;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.springframework.util.StringUtils;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {
    private static final String ROLE_ADMIN = "admin";
    private static final String ROLE_FARMER = "farmer";
    private static final String ROLE_CONSUMER = "consumer";
    
    private final AdminMapper adminMapper;
    private final ConsumerUserMapper consumerUserMapper;
    private final FarmerMapper farmerMapper;
    private final SecurityProperties securityProperties;

    public AuthServiceImpl(AdminMapper adminMapper,
                   ConsumerUserMapper consumerUserMapper,
                   FarmerMapper farmerMapper,
                   SecurityProperties securityProperties) {
        this.adminMapper = adminMapper;
        this.consumerUserMapper = consumerUserMapper;
        this.farmerMapper = farmerMapper;
        this.securityProperties = securityProperties;
    }
    
    private String getSmsCode() {
        return securityProperties.getSmsVerification().getMockCode();
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
        ConsumerUser consumerUser = consumerUserMapper.selectOne(new LambdaQueryWrapper<ConsumerUser>()
                .eq(ConsumerUser::getLoginName, loginDTO.getUsername())
                .last("limit 1"));
        if (consumerUser != null) {
            return loginConsumer(consumerUser, loginDTO.getPassword());
        }
        throw new BusinessException("用户名或密码错误");
    }

    @Override
    public LoginVO phoneLogin(String phone, String code, String role) {
        if (!StringUtils.hasText(phone)) {
            throw new BusinessException("手机号不能为空");
        }
        if (!StringUtils.hasText(code)) {
            throw new BusinessException("验证码不能为空");
        }
        if (!getSmsCode().equals(code)) {
            throw new BusinessException("验证码错误");
        }

        String loginRole = StringUtils.hasText(role) ? role.trim().toLowerCase() : ROLE_CONSUMER;
        if (ROLE_ADMIN.equals(loginRole)) {
            throw new BusinessException("管理员账号仅支持后台登录");
        }

        if (ROLE_FARMER.equals(loginRole)) {
            Farmer farmer = farmerMapper.selectOne(new LambdaQueryWrapper<Farmer>()
                    .eq(Farmer::getLoginName, phone)
                    .last("limit 1"));
            if (farmer == null) {
                farmer = new Farmer();
                farmer.setLoginName(phone);
                farmer.setPassword(PasswordUtil.encode(code));
                farmer.setFarmerName("农户" + maskPhone(phone));
                farmer.setContactPhone(phone);
                farmer.setOriginPlace("未设置");
                farmer.setAuthStatus(1);
                farmer.setAccountStatus(1);
                farmer.setRemark("测试阶段手机号快捷登录自动创建");
                farmerMapper.insert(farmer);
            } else if (PasswordUtil.needsUpgrade(farmer.getPassword())) {
                farmer.setPassword(PasswordUtil.encode(code));
                farmerMapper.updateById(farmer);
            }
            return loginFarmer(farmer, code);
        }

        ConsumerUser consumerUser = consumerUserMapper.selectOne(new LambdaQueryWrapper<ConsumerUser>()
                .eq(ConsumerUser::getLoginName, phone)
                .last("limit 1"));
        if (consumerUser == null) {
            consumerUser = new ConsumerUser();
            consumerUser.setLoginName(phone);
            consumerUser.setPassword(PasswordUtil.encode(code));
            consumerUser.setNickName("用户" + maskPhone(phone));
            consumerUser.setPhone(phone);
            consumerUser.setAvatar("");
            consumerUser.setAddress("");
            consumerUser.setStatus(1);
            consumerUser.setRemark("测试阶段手机号快捷登录自动创建");
            consumerUserMapper.insert(consumerUser);
        } else if (PasswordUtil.needsUpgrade(consumerUser.getPassword())) {
            consumerUser.setPassword(PasswordUtil.encode(code));
            consumerUserMapper.updateById(consumerUser);
        }
        return loginConsumer(consumerUser, code);
    }

    @Override
    public LoginVO wechatQuickLogin(String wechatCode, String nickName) {
        if (!StringUtils.hasText(wechatCode)) {
            throw new BusinessException("微信code不能为空");
        }
        String loginName = "wx_" + wechatCode.trim();
        ConsumerUser consumerUser = consumerUserMapper.selectOne(new LambdaQueryWrapper<ConsumerUser>()
                .eq(ConsumerUser::getLoginName, loginName)
                .last("limit 1"));
        if (consumerUser == null) {
            consumerUser = new ConsumerUser();
            consumerUser.setLoginName(loginName);
            consumerUser.setPassword(PasswordUtil.encode(wechatCode));
            consumerUser.setNickName(StringUtils.hasText(nickName) ? nickName : "微信用户");
            consumerUser.setPhone("WX");
            consumerUser.setAvatar("");
            consumerUser.setAddress("");
            consumerUser.setStatus(1);
            consumerUser.setRemark("测试阶段微信快捷登录自动创建");
            consumerUserMapper.insert(consumerUser);
        }
        return loginConsumer(consumerUser, wechatCode);
    }

    @Override
    public void registerByPhone(String phone, String code, String nickName, String role) {
        if (!StringUtils.hasText(phone) || !StringUtils.hasText(code)) {
            throw new BusinessException("手机号和验证码不能为空");
        }
        if (!getSmsCode().equals(code)) {
            throw new BusinessException("验证码错误");
        }
        Farmer farmerExists = farmerMapper.selectOne(new LambdaQueryWrapper<Farmer>()
                .eq(Farmer::getLoginName, phone)
                .last("limit 1"));
        if (farmerExists != null) {
            throw new BusinessException("该手机号已注册");
        }

        Admin adminExists = adminMapper.selectOne(new LambdaQueryWrapper<Admin>()
                .eq(Admin::getUsername, phone)
                .last("limit 1"));
        if (adminExists != null) {
            throw new BusinessException("该手机号已注册");
        }

        boolean requestAdmin = ROLE_ADMIN.equalsIgnoreCase(role);
        if (requestAdmin) {
            throw new BusinessException("管理员账号仅支持后台创建");
        }

        String normalizedRole = StringUtils.hasText(role) ? role.trim().toLowerCase() : ROLE_CONSUMER;
        if (ROLE_FARMER.equals(normalizedRole)) {
            Farmer farmer = new Farmer();
            farmer.setLoginName(phone);
            farmer.setPassword(PasswordUtil.encode(code));
            farmer.setFarmerName(StringUtils.hasText(nickName) ? nickName : "农户" + maskPhone(phone));
            farmer.setContactPhone(phone);
            farmer.setOriginPlace("未设置");
            farmer.setAuthStatus(1);
            farmer.setAccountStatus(1);
            farmer.setRemark("测试阶段模拟注册，角色=farmer");
            farmerMapper.insert(farmer);
            return;
        }
        if (!ROLE_CONSUMER.equals(normalizedRole)) {
            throw new BusinessException("role仅支持consumer或farmer");
        }
        ConsumerUser consumer = new ConsumerUser();
        consumer.setLoginName(phone);
        consumer.setPassword(PasswordUtil.encode(code));
        consumer.setNickName(StringUtils.hasText(nickName) ? nickName : "用户" + maskPhone(phone));
        consumer.setPhone(phone);
        consumer.setAvatar("");
        consumer.setAddress("");
        consumer.setStatus(1);
        consumer.setRemark("测试阶段模拟注册，角色=consumer");
        consumerUserMapper.insert(consumer);
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

    private LoginVO loginConsumer(ConsumerUser consumerUser, String rawPassword) {
        if (!PasswordUtil.matches(rawPassword, consumerUser.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        if (consumerUser.getStatus() != null && consumerUser.getStatus() == 0) {
            throw new BusinessException("当前用户已被禁用");
        }
        if (PasswordUtil.needsUpgrade(consumerUser.getPassword())) {
            consumerUser.setPassword(PasswordUtil.encode(rawPassword));
            consumerUserMapper.updateById(consumerUser);
        }
        return new LoginVO(consumerUser.getId(), consumerUser.getLoginName(), consumerUser.getNickName(), RoleEnum.CONSUMER.name());
    }

    private String maskPhone(String phone) {
        if (phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    @Override
    public Map<String, Object> getUserProfile(Long userId, String roleCode) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", userId);
        
        if ("FARMER".equalsIgnoreCase(roleCode)) {
            Farmer farmer = farmerMapper.selectById(userId);
            if (farmer != null) {
                data.put("name", farmer.getFarmerName());
                data.put("phone", farmer.getContactPhone());
                data.put("address", farmer.getOriginPlace());
                data.put("description", farmer.getDescription());
                data.put("avatar", "");
            }
        } else if ("CONSUMER".equalsIgnoreCase(roleCode)) {
            ConsumerUser consumer = consumerUserMapper.selectById(userId);
            if (consumer != null) {
                data.put("name", consumer.getNickName());
                data.put("phone", consumer.getPhone());
                data.put("address", consumer.getAddress());
                data.put("description", consumer.getDescription());
                data.put("avatar", consumer.getAvatar());
            }
        } else {
            Admin admin = adminMapper.selectById(userId);
            if (admin != null) {
                data.put("name", admin.getRealName());
                data.put("phone", admin.getPhone());
                data.put("address", "");
                data.put("description", "");
                data.put("avatar", "");
            }
        }
        
        return data;
    }
}