import { api } from './client'
import type { CollabTokenResponse, Note, NoteMember, NoteMemberRequest, NoteRequest } from './types'

export const listNotes = () => api.get<Note[]>('/api/notes')

export const createNote = (body: NoteRequest) => api.post<Note>('/api/notes', body)

export const getNote = (id: number) => api.get<Note>(`/api/notes/${id}`)

export const updateNote = (id: number, body: NoteRequest) => api.put<Note>(`/api/notes/${id}`, body)

export const deleteNote = (id: number) => api.delete(`/api/notes/${id}`)

export const listNoteMembers = (id: number) => api.get<NoteMember[]>(`/api/notes/${id}/members`)

export const addNoteMember = (id: number, body: NoteMemberRequest) =>
  api.post<NoteMember>(`/api/notes/${id}/members`, body)

export const removeNoteMember = (id: number, userId: number) =>
  api.delete(`/api/notes/${id}/members/${userId}`)

export const createCollabToken = (id: number) => api.post<CollabTokenResponse>(`/api/notes/${id}/collab-token`)
