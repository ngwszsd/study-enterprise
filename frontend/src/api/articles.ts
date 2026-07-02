import { api } from './client'
import type { Article, ArticleRequest, CategoryCount, PageResponse } from './types'

export const listArticles = (page: number, size: number, keyword: string) =>
  api.get<PageResponse<Article>>('/api/articles', { params: { page, size, keyword: keyword || undefined } })

export const getArticle = (id: number) => api.get<Article>(`/api/articles/${id}`)

export const createArticle = (body: ArticleRequest) => api.post<Article>('/api/articles', body)

export const updateArticle = (id: number, body: ArticleRequest) =>
  api.put<Article>(`/api/articles/${id}`, body)

export const deleteArticle = (id: number) => api.delete(`/api/articles/${id}`)

export const getStats = () => api.get<CategoryCount[]>('/api/articles/stats')
