# 前端统一联调清单（Session 版）

适用范围：当前项目采用 Session + Cookie 鉴权（非 JWT）。

## 一、必须统一项

1. 统一 baseURL
- 全项目只使用一个后端地址。
- 禁止混用 localhost、局域网 IP、花生壳域名。

2. 统一请求入口
- 所有页面接口调用只能通过同一个 request 封装。
- 禁止页面内直接分散调用 wx.request。

3. 统一登录态传递
- 登录成功后保存 Set-Cookie（JSESSIONID）。
- 后续受保护接口自动携带 Cookie。

4. 统一错误处理
- code = 0 视为成功。
- code = 401 统一跳登录并清理本地会话。
- 其他非 0 错误统一弹 message。

5. 统一调用时机
- 登录成功后再请求受保护接口。
- 避免 onLoad 并发请求先于登录完成导致 401。

## 二、请求封装必查清单

1. 登录接口返回后，响应头是否有 Set-Cookie。
2. 本地是否已保存 Cookie。
3. 受保护接口请求头是否带 Cookie。
4. 所有请求是否都走同一 baseURL。
5. 是否错误使用 Authorization: Bearer（当前后端不依赖该头鉴权）。

## 三、最小请求封装示例（微信小程序）

文件：utils/request.js

const BASE_URL = 'http://你的花生壳域名'
const COOKIE_KEY = 'SESSION_COOKIE'

function getCookie() {
  return wx.getStorageSync(COOKIE_KEY) || ''
}

function saveCookieFromResponse(res) {
  const setCookie = res?.header?.['Set-Cookie'] || res?.header?.['set-cookie']
  if (setCookie) {
    wx.setStorageSync(COOKIE_KEY, setCookie)
  }
}

function request({ url, method = 'GET', data, header = {} }) {
  return new Promise((resolve, reject) => {
    const cookie = getCookie()
    wx.request({
      url: `${BASE_URL}${url}`,
      method,
      data,
      header: {
        'content-type': 'application/json',
        ...(cookie ? { Cookie: cookie } : {}),
        ...header
      },
      success(res) {
        saveCookieFromResponse(res)
        const body = res.data || {}
        if (body.code === 0) {
          resolve(body.data)
          return
        }
        if (body.code === 401) {
          wx.removeStorageSync(COOKIE_KEY)
          wx.showToast({ title: '请先登录', icon: 'none' })
          // TODO: 跳转登录页
        }
        reject(body)
      },
      fail: reject
    })
  })
}

export default request

## 四、登录与鉴权联调步骤（必须按顺序）

1. 调用手机号登录
- POST /api/auth/phone-login
- body: { phone, code, role }

2. 立即调用当前用户接口
- GET /api/auth/me
- 预期：code = 0

3. 再调用一个受保护业务接口
- 例如 GET /api/addresses
- 预期：code = 0

4. 刷新页面后重复第 2、3 步
- 验证会话保持是否正常

## 五、角色分流约定

登录成功后，前端页面分流统一读取：
- data.userInfo.role

可选值：
- consumer：普通用户端页面
- farmer：农户端页面
- admin：后台角色

## 六、当前后端接口约定（认证相关）

1. POST /api/auth/sms
- 发送验证码（测试阶段固定验证码 123456）

2. POST /api/auth/register
- 注册（仅支持 consumer/farmer）
- role = admin 会被拒绝（管理员仅后台创建）

3. POST /api/auth/phone-login
- 小程序主登录入口（推荐）

4. POST /api/auth/login
- 账号密码登录（兼容/后台）

5. GET /api/auth/me
- 获取当前登录用户

6. POST /api/auth/logout
- 退出登录

## 七、常见故障对照

1. 现象：登录成功，但业务接口 401
- 原因：后续请求没有携带 Cookie 或混用域名。

2. 现象：偶发 401，且日志出现多条 UnauthorizedException
- 原因：页面并发请求中部分请求未带会话。

3. 现象：本地可用，真机不稳定
- 原因：请求封装不统一、环境地址不统一。

---

维护建议：
- 后续若迁移 JWT，再新增一份 JWT 版联调清单，不要与 Session 版混用。
