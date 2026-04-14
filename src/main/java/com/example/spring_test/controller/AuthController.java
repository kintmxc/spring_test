package com.example.spring_test.controller;

import com.example.spring_test.common.Result;
import com.example.spring_test.dto.LoginDTO;
import com.example.spring_test.dto.PhoneLoginDTO;
import com.example.spring_test.dto.WechatLoginDTO;
import com.example.spring_test.security.LoginInterceptor;
import com.example.spring_test.security.SessionUser;
import com.example.spring_test.security.SessionUserHolder;
import com.example.spring_test.service.AuthService;
import com.example.spring_test.vo.LoginVO;
import com.example.spring_test.vo.LoginUserInfoVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/sms")
    public Result<Void> sendSms(@RequestBody Map<String, String> body) {
        String phone = readField(body, "phone");
        if (!StringUtils.hasText(phone)) {
            return Result.fail("手机号不能为空");
        }
        // 测试阶段固定验证码，后续可替换为真实短信服务
        return Result.success("验证码已发送", null);
    }

    @PostMapping("/register")
    public Result<Void> register(@RequestBody Map<String, Object> body) {
        String phone = readField(body, "phone");
        String code = readField(body, "code");
        String nickName = readField(body, "nickName");
        String role = readField(body, "role");
        if (!StringUtils.hasText(phone) || !StringUtils.hasText(code)) {
            return Result.fail("手机号和验证码不能为空");
        }
        authService.registerByPhone(phone, code, nickName, role);
        return Result.success("注册成功", null);
    }

    @PostMapping("/phone-login")
    public Result<LoginVO> phoneLogin(@RequestBody PhoneLoginDTO phoneLoginDTO, HttpServletRequest request) {
        if (phoneLoginDTO == null || !StringUtils.hasText(phoneLoginDTO.getPhone()) || !StringUtils.hasText(phoneLoginDTO.getCode())) {
            return Result.fail("手机号和验证码不能为空");
        }
        LoginVO loginVO = authService.phoneLogin(phoneLoginDTO.getPhone(), phoneLoginDTO.getCode(), phoneLoginDTO.getRole());
        buildSessionAndPayload(loginVO, request);
        return Result.success("登录成功", loginVO);
    }

    @PostMapping("/wechat-login")
    public Result<LoginVO> wechatLogin(@RequestBody WechatLoginDTO wechatLoginDTO, HttpServletRequest request) {
        if (wechatLoginDTO == null || !StringUtils.hasText(wechatLoginDTO.getCode())) {
            return Result.fail("微信code不能为空");
        }
        LoginVO loginVO = authService.wechatQuickLogin(wechatLoginDTO.getCode(), wechatLoginDTO.getNickName());
        buildSessionAndPayload(loginVO, request);
        return Result.success("登录成功", loginVO);
    }

    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody LoginDTO loginDTO, HttpServletRequest request) {
        if (loginDTO == null) {
            return Result.fail("请求参数不能为空");
        }
        if (!StringUtils.hasText(loginDTO.getUsername()) && StringUtils.hasText(loginDTO.getPhone())) {
            loginDTO.setUsername(loginDTO.getPhone());
        }
        if (!StringUtils.hasText(loginDTO.getPassword()) && StringUtils.hasText(loginDTO.getCode())) {
            loginDTO.setPassword(loginDTO.getCode());
        }
        if (!StringUtils.hasText(loginDTO.getUsername()) || !StringUtils.hasText(loginDTO.getPassword())) {
            return Result.fail("用户名和密码不能为空");
        }
        LoginVO loginVO = authService.login(loginDTO);
        if (!"ADMIN".equalsIgnoreCase(loginVO.getRoleCode())) {
            return Result.fail(403, "管理端仅支持管理员登录，农户/消费者请使用小程序端");
        }
        buildSessionAndPayload(loginVO, request);

        return Result.success("登录成功", loginVO);
    }

    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return Result.success("退出成功", null);
    }

    @GetMapping("/me")
    public Result<Map<String, Object>> currentUser() {
        SessionUser user = SessionUserHolder.get();
        if (user == null) {
            return Result.fail(401, "未登录或登录已过期");
        }
        return Result.success(buildCurrentUserPayload(user));
    }

    @GetMapping("/user-info")
    public Result<Map<String, Object>> userInfo() {
        return currentUser();
    }

    private void buildSessionAndPayload(LoginVO loginVO, HttpServletRequest request) {
        HttpSession session = request.getSession();
        session.setAttribute(LoginInterceptor.SESSION_KEY,
                new SessionUser(loginVO.getUserId(), loginVO.getUsername(), loginVO.getRoleCode()));
        loginVO.setToken(session.getId());
        Map<String, Object> profile = authService.getUserProfile(loginVO.getUserId(), loginVO.getRoleCode());
        loginVO.setUserInfo(buildLoginUserInfo(loginVO, profile));
    }

    private LoginUserInfoVO buildLoginUserInfo(LoginVO loginVO, Map<String, Object> profile) {
        LoginUserInfoVO userInfo = new LoginUserInfoVO();
        userInfo.setId(loginVO.getUserId());
        userInfo.setPhone(asString(profile.get("phone"), loginVO.getUsername()));
        userInfo.setNickName(asString(profile.get("name"), loginVO.getRealName()));
        userInfo.setRole(resolveRole(loginVO.getRoleCode()));
        userInfo.setAvatar(asString(profile.get("avatar"), ""));
        userInfo.setAddress(asString(profile.get("address"), ""));
        userInfo.setUsername(loginVO.getUsername());
        userInfo.setRealName(loginVO.getRealName());
        userInfo.setRoleCode(loginVO.getRoleCode());
        return userInfo;
    }

    private Map<String, Object> buildCurrentUserPayload(SessionUser user) {
        Map<String, Object> data = authService.getUserProfile(user.getUserId(), user.getRoleCode());
        String displayName = asString(data.get("name"), user.getUsername());
        data.put("id", user.getUserId());
        data.put("userId", user.getUserId());
        data.put("username", user.getUsername());
        data.put("roleCode", user.getRoleCode());
        data.put("role", resolveRole(user.getRoleCode()));
        data.put("name", displayName);
        data.put("nickName", displayName);
        data.put("realName", displayName);
        data.put("phone", asString(data.get("phone"), user.getUsername()));
        data.put("avatar", asString(data.get("avatar"), ""));
        data.put("address", asString(data.get("address"), ""));
        return data;
    }

    private String asString(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String text = String.valueOf(value);
        return StringUtils.hasText(text) ? text : defaultValue;
    }

    private String readField(Map<String, ?> body, String key) {
        if (body == null) {
            return null;
        }
        return asString(body.get(key), null);
    }

    private String resolveRole(String roleCode) {
        if ("FARMER".equalsIgnoreCase(roleCode)) {
            return "farmer";
        }
        if ("CONSUMER".equalsIgnoreCase(roleCode)) {
            return "consumer";
        }
        return "admin";
    }
}