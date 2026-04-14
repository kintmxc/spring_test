package com.example.spring_test.controller;

import com.example.spring_test.common.Result;
import com.example.spring_test.dto.AddressSaveDTO;
import com.example.spring_test.service.AddressService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/addresses")
public class AddressController {
    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping
    public Result<List<Map<String, Object>>> list() {
        return Result.success(addressService.listCurrentUserAddresses());
    }

    @PostMapping
    public Result<Map<String, Object>> create(@RequestBody AddressSaveDTO addressSaveDTO) {
        return Result.success("新增地址成功", addressService.create(addressSaveDTO));
    }

    @PutMapping("/{id}")
    public Result<Map<String, Object>> update(@PathVariable Long id, @RequestBody AddressSaveDTO addressSaveDTO) {
        return Result.success("更新地址成功", addressService.update(id, addressSaveDTO));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        addressService.delete(id);
        return Result.success("删除地址成功", null);
    }
}
