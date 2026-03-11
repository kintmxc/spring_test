import { http, request } from './http'

export const orderPageApi = (params) => request(http.get('/api/orders', { params }))
export const orderDetailApi = (id) => request(http.get(`/api/orders/${id}`))
export const updateOrderStatusApi = (id, payload) => request(http.put(`/api/orders/${id}/status`, payload))
export const shipOrderApi = (id, payload) => request(http.put(`/api/orders/${id}/ship`, payload))
export const cancelOrderApi = (id, remark) => request(http.post(`/api/orders/${id}/cancel`, null, { params: { remark } }))