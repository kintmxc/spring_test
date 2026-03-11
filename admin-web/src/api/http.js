import axios from 'axios'

const http = axios.create({
  baseURL: '/',
  timeout: 12000,
  withCredentials: true,
})

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

export { http, request }