import { api } from './client'
import type { FileResponse } from './types'

export const uploadFile = (file: File) => {
  const form = new FormData()
  form.append('file', file)
  return api.post<FileResponse>('/api/files', form)
}
