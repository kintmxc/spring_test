import { http, request } from './http'

export const uploadImageApi = async (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request(http.post('/api/files/image', formData))
}