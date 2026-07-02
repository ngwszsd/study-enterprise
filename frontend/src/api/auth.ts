import { api } from './client'
import type { AuthResponse, UserResponse } from './types'

export const register = (username: string, password: string) =>
  api.post<UserResponse>('/api/auth/register', { username, password })

export const login = (username: string, password: string) =>
  api.post<AuthResponse>('/api/auth/login', { username, password })

export const me = () => api.get<UserResponse>('/api/auth/me')
