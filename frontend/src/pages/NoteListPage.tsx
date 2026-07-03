import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { createNote, deleteNote, listNotes } from '../api/notes'
import { errorMessage } from '../api/client'
import type { Note } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { Button, ErrorText, GhostButton, Input, Spinner } from '../components/ui'

export default function NoteListPage() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const [notes, setNotes] = useState<Note[]>([])
  const [title, setTitle] = useState('')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  const load = () => {
    setLoading(true)
    setError('')
    listNotes()
      .then((r) => setNotes(r.data))
      .catch((e) => setError(errorMessage(e)))
      .finally(() => setLoading(false))
  }

  useEffect(load, [])

  const onCreate = async (e: FormEvent) => {
    e.preventDefault()
    const value = title.trim()
    if (!value) return
    setSaving(true)
    setError('')
    try {
      const { data } = await createNote({ title: value })
      navigate(`/notes/${data.id}`)
    } catch (err) {
      setError(errorMessage(err, '创建笔记失败'))
    } finally {
      setSaving(false)
    }
  }

  const onDelete = async (note: Note) => {
    if (note.role !== 'OWNER') return
    if (!window.confirm(`删除笔记「${note.title}」？`)) return
    await deleteNote(note.id)
    load()
  }

  return (
    <div className="mx-auto max-w-4xl px-4 py-6">
      <header className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">协作笔记</h1>
          <p className="mt-1 text-sm text-gray-500">Yjs + Hocuspocus + Spring 鉴权持久化</p>
        </div>
        <div className="flex items-center gap-3 text-sm">
          <span className="text-gray-500">你好,{user?.username}</span>
          <Link to="/articles" className="text-indigo-600 hover:underline">
            文章
          </Link>
        </div>
      </header>

      <form onSubmit={onCreate} className="mb-5 flex gap-2">
        <Input
          placeholder="新笔记标题"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          maxLength={200}
        />
        <Button type="submit" disabled={saving}>
          {saving ? '创建中…' : '新建笔记'}
        </Button>
      </form>
      <ErrorText>{error}</ErrorText>

      {loading ? (
        <Spinner />
      ) : notes.length === 0 ? (
        <p className="rounded-lg border border-dashed border-gray-200 p-8 text-center text-gray-400">
          暂无笔记，先创建一篇。
        </p>
      ) : (
        <ul className="mt-4 grid gap-3">
          {notes.map((note) => (
            <li key={note.id} className="rounded-lg border border-gray-200 bg-white p-4">
              <div className="flex items-start justify-between gap-3">
                <Link to={`/notes/${note.id}`} className="min-w-0 flex-1">
                  <h2 className="truncate font-semibold text-gray-900">{note.title}</h2>
                  <div className="mt-2 flex flex-wrap gap-3 text-xs text-gray-400">
                    <span>拥有者 {note.ownerUsername}</span>
                    <span>{note.role}</span>
                    <span>{note.updatedAt.slice(0, 10)}</span>
                  </div>
                </Link>
                {note.role === 'OWNER' && (
                  <GhostButton onClick={() => onDelete(note)} className="shrink-0">
                    删除
                  </GhostButton>
                )}
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
