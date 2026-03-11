import { http, request } from './http'

export const loginApi = (payload) => request(http.post('/api/auth/login', payload))
export const logoutApi = () => request(http.post('/api/auth/logout'))
export const meApi = () => request(http.get('/api/auth/me'))