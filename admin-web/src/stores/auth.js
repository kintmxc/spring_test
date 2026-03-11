import { reactive } from 'vue'
import { loginApi, logoutApi, meApi } from '../api/auth'

export const authState = reactive({
  user: null,
  ready: false,
})

export async function fetchCurrentUser() {
  const user = await meApi()
  authState.user = user
  authState.ready = true
  return user
}

export async function login(payload) {
  const user = await loginApi(payload)
  authState.user = user
  authState.ready = true
  return user
}

export async function logout() {
  await logoutApi()
  clearAuth()
}

export function clearAuth() {
  authState.user = null
  authState.ready = true
}