import { http, request } from './http'

export const farmerPageApi = (params) => request(http.get('/api/farmers', { params }))
export const farmerDetailApi = (id) => request(http.get(`/api/farmers/${id}`))
export const farmerOptionsApi = () => request(http.get('/api/farmers/options'))
export const createFarmerApi = (payload) => request(http.post('/api/farmers', payload))
export const updateFarmerApi = (id, payload) => request(http.put(`/api/farmers/${id}`, payload))
export const auditFarmerApi = (id, payload) => request(http.put(`/api/farmers/${id}/audit`, payload))
export const updateFarmerStatusApi = (id, accountStatus) => request(http.put(`/api/farmers/${id}/status`, null, { params: { accountStatus } }))