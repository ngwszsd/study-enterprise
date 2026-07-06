import axios from 'axios'
import { clearToken, getToken } from '../auth/token'
import type { ApiError } from './types'

const baseURL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:18080'

export const api = axios.create({ baseURL })

// 请求拦截:自动带上 JWT
api.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截:401 统一清 token 跳登录
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      clearToken()
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  },
)

/** 从 axios 错误里取后端的可读信息。 */
export function errorMessage(error: unknown, fallback = '请求失败'): string {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as ApiError | undefined
    if (data?.message) return data.message
    if (error.message) return error.message
  }
  return fallback
}
