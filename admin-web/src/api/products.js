import { http, request } from './http'

export const productPageApi = (params) => request(http.get('/api/products', { params }))
export const productDetailApi = (id) => request(http.get(`/api/products/${id}`))
export const createProductApi = (payload) => request(http.post('/api/products', payload))
export const updateProductApi = (id, payload) => request(http.put(`/api/products/${id}`, payload))
export const updateProductStatusApi = (id, saleStatus) => request(http.put(`/api/products/${id}/sale-status`, null, { params: { saleStatus } }))
export const updateProductStockApi = (id, payload) => request(http.put(`/api/products/${id}/stock`, payload))
export const deleteProductApi = (id) => request(http.delete(`/api/products/${id}`))