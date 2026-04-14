package com.example.spring_test.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.spring_test.dto.AddressSaveDTO;
import com.example.spring_test.entity.UserAddress;
import com.example.spring_test.exception.BusinessException;
import com.example.spring_test.mapper.UserAddressMapper;
import com.example.spring_test.security.CurrentUserUtil;
import com.example.spring_test.service.AddressService;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AddressServiceImpl implements AddressService {
    private final UserAddressMapper userAddressMapper;

    public AddressServiceImpl(UserAddressMapper userAddressMapper) {
        this.userAddressMapper = userAddressMapper;
    }

    @Override
    public List<Map<String, Object>> listCurrentUserAddresses() {
        Long userId = CurrentUserUtil.currentUserId();
        List<UserAddress> addresses = userAddressMapper.selectList(new LambdaQueryWrapper<UserAddress>()
                .eq(UserAddress::getUserId, userId)
                .orderByDesc(UserAddress::getIsDefault)
                .orderByDesc(UserAddress::getId));
        return addresses.stream().map(this::toView).sorted(Comparator.comparing(item -> (Long) item.get("id"))).toList();
    }

    @Override
    @Transactional
    public Map<String, Object> create(AddressSaveDTO addressSaveDTO) {
        validate(addressSaveDTO);
        Long userId = CurrentUserUtil.currentUserId();
        boolean makeDefault = Boolean.TRUE.equals(addressSaveDTO.getDefaultFlag());

        Long count = userAddressMapper.selectCount(new LambdaQueryWrapper<UserAddress>()
                .eq(UserAddress::getUserId, userId));
        if (count != null && count == 0) {
            makeDefault = true;
        }
        if (makeDefault) {
            clearDefault(userId);
        }

        UserAddress address = new UserAddress();
        address.setUserId(userId);
        fill(address, addressSaveDTO);
        address.setIsDefault(makeDefault ? 1 : 0);
        userAddressMapper.insert(address);
        return toView(address);
    }

    @Override
    @Transactional
    public Map<String, Object> update(Long id, AddressSaveDTO addressSaveDTO) {
        validate(addressSaveDTO);
        Long userId = CurrentUserUtil.currentUserId();
        UserAddress address = getOwnedAddress(id, userId);
        if (Boolean.TRUE.equals(addressSaveDTO.getDefaultFlag())) {
            clearDefault(userId);
            address.setIsDefault(1);
        } else if (address.getIsDefault() == null) {
            address.setIsDefault(0);
        }
        fill(address, addressSaveDTO);
        userAddressMapper.updateById(address);
        return toView(address);
    }

    @Override
    public void delete(Long id) {
        Long userId = CurrentUserUtil.currentUserId();
        UserAddress address = getOwnedAddress(id, userId);
        userAddressMapper.deleteById(address.getId());

        if (address.getIsDefault() != null && address.getIsDefault() == 1) {
            UserAddress next = userAddressMapper.selectOne(new LambdaQueryWrapper<UserAddress>()
                    .eq(UserAddress::getUserId, userId)
                    .orderByDesc(UserAddress::getId)
                    .last("limit 1"));
            if (next != null) {
                next.setIsDefault(1);
                userAddressMapper.updateById(next);
            }
        }
    }

    @Override
    public UserAddress getById(Long id) {
        return userAddressMapper.selectById(id);
    }

    private UserAddress getOwnedAddress(Long id, Long userId) {
        UserAddress address = userAddressMapper.selectById(id);
        if (address == null || !userId.equals(address.getUserId())) {
            throw new BusinessException("地址不存在");
        }
        return address;
    }

    private void clearDefault(Long userId) {
        List<UserAddress> addresses = userAddressMapper.selectList(new LambdaQueryWrapper<UserAddress>()
                .eq(UserAddress::getUserId, userId)
                .eq(UserAddress::getIsDefault, 1));
        for (UserAddress item : addresses) {
            item.setIsDefault(0);
            userAddressMapper.updateById(item);
        }
    }

    private void validate(AddressSaveDTO dto) {
        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new BusinessException("收货人不能为空");
        }
        if (dto.getPhone() == null || dto.getPhone().isBlank()) {
            throw new BusinessException("联系电话不能为空");
        }
        if (dto.getDetail() == null || dto.getDetail().isBlank()) {
            throw new BusinessException("详细地址不能为空");
        }
    }

    private void fill(UserAddress entity, AddressSaveDTO dto) {
        entity.setName(dto.getName());
        entity.setPhone(dto.getPhone());
        entity.setProvince(dto.getProvince());
        entity.setCity(dto.getCity());
        entity.setDistrict(dto.getDistrict());
        entity.setDetail(dto.getDetail());
    }

    private Map<String, Object> toView(UserAddress address) {
        return Map.of(
                "id", address.getId(),
                "name", valueOrEmpty(address.getName()),
                "phone", valueOrEmpty(address.getPhone()),
                "province", valueOrEmpty(address.getProvince()),
                "city", valueOrEmpty(address.getCity()),
                "district", valueOrEmpty(address.getDistrict()),
                "detail", valueOrEmpty(address.getDetail()),
                "isDefault", address.getIsDefault() != null && address.getIsDefault() == 1
        );
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
