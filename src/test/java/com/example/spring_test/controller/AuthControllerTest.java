package com.example.spring_test.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.spring_test.common.Result;
import com.example.spring_test.dto.LoginDTO;
import com.example.spring_test.security.SessionUser;
import com.example.spring_test.security.SessionUserHolder;
import com.example.spring_test.service.AuthService;
import com.example.spring_test.vo.LoginVO;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    private AuthController authController;

    @BeforeEach
    void setUp() {
        authController = new AuthController(authService);
    }

    @AfterEach
    void tearDown() {
        SessionUserHolder.clear();
    }

    @Test
    void login_shouldFail_whenRequestBodyIsNull() {
        Result<LoginVO> result = authController.login(null, new MockHttpServletRequest());

        assertFalse(result.isSuccess());
        assertEquals(400, result.getCode());
        assertEquals("请求参数不能为空", result.getMessage());
    }

    @Test
    void login_shouldNormalizePhoneAndCode_beforeCallingService() {
        LoginDTO dto = new LoginDTO();
        dto.setPhone("13800000000");
        dto.setCode("123456");

        LoginVO loginVO = new LoginVO(1L, "admin", "管理员", "ADMIN");
        when(authService.login(any(LoginDTO.class))).thenReturn(loginVO);
        when(authService.getUserProfile(1L, "ADMIN")).thenReturn(new HashMap<>());

        Result<LoginVO> result = authController.login(dto, new MockHttpServletRequest());

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        ArgumentCaptor<LoginDTO> captor = ArgumentCaptor.forClass(LoginDTO.class);
        verify(authService).login(captor.capture());
        assertEquals("13800000000", captor.getValue().getUsername());
        assertEquals("123456", captor.getValue().getPassword());
    }

    @Test
    void currentUser_shouldReturnUnifiedFields() {
        SessionUserHolder.set(new SessionUser(99L, "consumer01", "CONSUMER"));

        Map<String, Object> profile = new HashMap<>();
        profile.put("name", "测试用户");
        profile.put("phone", "13800000000");
        when(authService.getUserProfile(99L, "CONSUMER")).thenReturn(profile);

        Result<Map<String, Object>> result = authController.currentUser();

        assertTrue(result.isSuccess());
        assertEquals(0, result.getCode());
        Map<String, Object> data = result.getData();
        assertEquals("consumer", data.get("role"));
        assertEquals("测试用户", data.get("name"));
        assertEquals("测试用户", data.get("nickName"));
        assertEquals("测试用户", data.get("realName"));
        assertEquals("13800000000", data.get("phone"));
    }
}
