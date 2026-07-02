// 与后端 DTO 对齐的类型

export interface UserResponse {
  id: number
  username: string
}

export interface AuthResponse {
  token: string
  tokenType: string
  expiresIn: number
}

export interface Article {
  id: number
  title: string
  content: string
  category: string | null
  coverImageKey: string | null
  coverImageUrl: string | null
  authorId: number
  authorUsername: string | null
  createdAt: string
  updatedAt: string
  viewCount: number
}

export interface ArticleRequest {
  title: string
  content: string
  category?: string | null
  coverImageKey?: string | null
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface FileResponse {
  key: string
  url: string | null
}

export interface ApiError {
  code: string
  message: string
  errors?: Record<string, string>
}

export interface CategoryCount {
  category: string
  count: number
}
