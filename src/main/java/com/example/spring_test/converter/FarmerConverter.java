package com.example.spring_test.converter;

import com.example.spring_test.entity.Farmer;
import com.example.spring_test.vo.FarmerDetailVO;
import com.example.spring_test.vo.FarmerListVO;
import com.example.spring_test.vo.OptionVO;
import org.springframework.stereotype.Component;

@Component
public class FarmerConverter {

    public FarmerDetailVO toDetailVO(Farmer farmer, int productCount, 
                                       int onSaleProductCount, int orderCount) {
        FarmerDetailVO vo = new FarmerDetailVO();
        vo.setId(farmer.getId());
        vo.setLoginName(farmer.getLoginName());
        vo.setFarmerName(farmer.getFarmerName());
        vo.setContactPhone(farmer.getContactPhone());
        vo.setOriginPlace(farmer.getOriginPlace());
        vo.setIdCardNo(farmer.getIdCardNo());
        vo.setLicenseNo(farmer.getLicenseNo());
        vo.setAuthStatus(farmer.getAuthStatus());
        vo.setAuthStatusText(toAuthStatusText(farmer.getAuthStatus()));
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

    public FarmerListVO toListVO(Farmer farmer, int productCount, 
                                  int onSaleProductCount, int orderCount) {
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
        vo.setAuthStatusText(toAuthStatusText(farmer.getAuthStatus()));
        vo.setAccountStatusText(farmer.getAccountStatus() != null && farmer.getAccountStatus() == 1 ? "启用" : "禁用");
        vo.setProductCount(productCount);
        vo.setOnSaleProductCount(onSaleProductCount);
        vo.setOrderCount(orderCount);
        return vo;
    }

    public OptionVO toOptionVO(Farmer farmer) {
        return new OptionVO(farmer.getId(), farmer.getFarmerName());
    }

    public String toAuthStatusText(Integer authStatus) {
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
}
