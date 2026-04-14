package com.example.spring_test.service;

import com.example.spring_test.dto.AddressSaveDTO;
import java.util.List;
import java.util.Map;

public interface AddressService {
    List<Map<String, Object>> listCurrentUserAddresses();

    Map<String, Object> create(AddressSaveDTO addressSaveDTO);

    Map<String, Object> update(Long id, AddressSaveDTO addressSaveDTO);

    void delete(Long id);

    com.example.spring_test.entity.UserAddress getById(Long id);
}
