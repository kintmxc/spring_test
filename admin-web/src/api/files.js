import { http, request } from './http'

// 从环境变量获取上传URL
const uploadUrl = import.meta.env.VITE_UPLOAD_URL || '/api/files/image'

export const uploadImageApi = async (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request(http.post(uploadUrl, formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  }))
}

// 获取完整图片URL
export const getImageUrl = (path) => {
  if (!path) return ''
  // 如果已经是完整URL，直接返回
  if (path.startsWith('http://') || path.startsWith('https://')) {
    return path
  }
  // 拼接基础URL
  const baseUrl = import.meta.env.VITE_IMAGE_BASE_URL || ''
  return `${baseUrl}${path.startsWith('/') ? '' : '/'}${path}`
}
