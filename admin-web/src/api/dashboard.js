import { http, request } from './http'

export const overviewApi = () => request(http.get('/api/dashboard/overview'))
export const latestOrdersApi = (limit = 5) => request(http.get('/api/dashboard/latest-orders', { params: { limit } }))