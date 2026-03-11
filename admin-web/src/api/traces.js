import { http, request } from './http'

export const tracePageApi = (params) => request(http.get('/api/traces/page', { params }))
export const traceByProductApi = (productId) => request(http.get('/api/traces', { params: { productId } }))
export const saveTraceApi = (payload) => request(http.post('/api/traces', payload))
export const disableTraceApi = (id) => request(http.delete(`/api/traces/${id}`))