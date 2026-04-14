import axios from 'axios'

// 从环境变量获取API基础URL
const baseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081'

const http = axios.create({
  baseURL: baseURL,
  timeout: 12000,
  withCredentials: true,
})

// 请求拦截器 - 添加token等
http.interceptors.request.use(
  (config) => {
    // 可以在这里添加token
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器 - 统一错误处理
http.interceptors.response.use(
  (response) => {
    return response
  },
  (error) => {
    // 统一处理401未授权
    if (error.response?.status === 401) {
      // 清除登录状态
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      // 可以在这里跳转登录页
      console.warn('登录已过期，请重新登录')
    }
    return Promise.reject(error)
  }
)

function normalizeError(error) {
  return {
    status: error.response?.status ?? 500,
    message: error.response?.data?.message || error.message || '请求失败',
    data: error.response?.data ?? null,
  }
}

async function request(promise) {
  try {
    const response = await promise
    if (response.data?.success === false) {
      throw {
        response: {
          status: 400,
          data: response.data,
        },
      }
    }
    return response.data?.data
  } catch (error) {
    throw normalizeError(error)
  }
}

export { http, request, baseURL }
