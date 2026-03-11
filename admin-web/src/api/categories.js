import { http, request } from './http'

export const categoryListApi = () => request(http.get('/api/categories'))
export const categoryOptionsApi = () => request(http.get('/api/categories/options'))
export const createCategoryApi = (payload) => request(http.post('/api/categories', payload))
export const updateCategoryApi = (id, payload) => request(http.put(`/api/categories/${id}`, payload))
export const deleteCategoryApi = (id) => request(http.delete(`/api/categories/${id}`))