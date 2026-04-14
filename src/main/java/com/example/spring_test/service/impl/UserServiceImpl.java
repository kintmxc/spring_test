package com.example.spring_test.service.impl;

import com.example.spring_test.dto.UserProfileUpdateDTO;
import com.example.spring_test.entity.ConsumerUser;
import com.example.spring_test.entity.Farmer;
import com.example.spring_test.exception.BusinessException;
import com.example.spring_test.mapper.ConsumerUserMapper;
import com.example.spring_test.mapper.FarmerMapper;
import com.example.spring_test.security.SessionUser;
import com.example.spring_test.security.SessionUserHolder;
import com.example.spring_test.service.UserService;
import com.example.spring_test.vo.UserProfileVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {
    private final FarmerMapper farmerMapper;
    private final ConsumerUserMapper consumerUserMapper;

    public UserServiceImpl(FarmerMapper farmerMapper, ConsumerUserMapper consumerUserMapper) {
        this.farmerMapper = farmerMapper;
        this.consumerUserMapper = consumerUserMapper;
    }

    @Override
    public UserProfileVO getProfile() {
        SessionUser user = SessionUserHolder.get();
        if (user == null) {
            throw new BusinessException("未登录或登录已过期");
        }
        return buildProfileVO(user);
    }

    @Override
    @Transactional
    public UserProfileVO updateProfile(UserProfileUpdateDTO dto) {
        SessionUser user = SessionUserHolder.get();
        if (user == null) {
            throw new BusinessException("未登录或登录已过期");
        }
        
        String roleCode = user.getRoleCode();
        Long userId = user.getUserId();
        
        if ("FARMER".equalsIgnoreCase(roleCode)) {
            Farmer farmer = farmerMapper.selectById(userId);
            if (farmer == null) {
                throw new BusinessException("农户不存在");
            }
            if (dto.getName() != null) {
                farmer.setFarmerName(dto.getName());
            }
            if (dto.getPhone() != null) {
                farmer.setContactPhone(dto.getPhone());
            }
            if (dto.getAddress() != null) {
                farmer.setOriginPlace(dto.getAddress());
            }
            if (dto.getDescription() != null) {
                farmer.setDescription(dto.getDescription());
            }
            farmerMapper.updateById(farmer);
        } else if ("CONSUMER".equalsIgnoreCase(roleCode)) {
            ConsumerUser consumer = consumerUserMapper.selectById(userId);
            if (consumer == null) {
                throw new BusinessException("用户不存在");
            }
            if (dto.getName() != null) {
                consumer.setNickName(dto.getName());
            }
            if (dto.getPhone() != null) {
                consumer.setPhone(dto.getPhone());
            }
            if (dto.getAddress() != null) {
                consumer.setAddress(dto.getAddress());
            }
            if (dto.getDescription() != null) {
                consumer.setDescription(dto.getDescription());
            }
            consumerUserMapper.updateById(consumer);
        } else {
            throw new BusinessException("管理员暂不支持修改资料");
        }
        
        return buildProfileVO(user);
    }

    private UserProfileVO buildProfileVO(SessionUser user) {
        UserProfileVO vo = new UserProfileVO();
        vo.setId(user.getUserId());
        vo.setRoleCode(user.getRoleCode());
        
        String roleCode = user.getRoleCode();
        Long userId = user.getUserId();
        
        if ("FARMER".equalsIgnoreCase(roleCode)) {
            Farmer farmer = farmerMapper.selectById(userId);
            if (farmer != null) {
                vo.setName(farmer.getFarmerName());
                vo.setPhone(farmer.getContactPhone());
                vo.setAddress(farmer.getOriginPlace());
                vo.setDescription(farmer.getDescription());
            }
        } else if ("CONSUMER".equalsIgnoreCase(roleCode)) {
            ConsumerUser consumer = consumerUserMapper.selectById(userId);
            if (consumer != null) {
                vo.setName(consumer.getNickName());
                vo.setPhone(consumer.getPhone());
                vo.setAddress(consumer.getAddress());
                vo.setDescription(consumer.getDescription());
                vo.setAvatar(consumer.getAvatar());
            }
        }
        
        return vo;
    }
}
