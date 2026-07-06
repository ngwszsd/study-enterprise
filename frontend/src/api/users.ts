import { api } from './client'
import type { UserResponse } from './types'

export const searchUsers = (keyword: string) =>
  api.get<UserResponse[]>('/api/users', {
    params: { keyword: keyword.trim() || undefined },
  })
