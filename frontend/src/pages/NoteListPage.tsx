import { Card, Chip, Separator } from '@heroui/react'
import { NotebookPen, Plus, RadioTower, Trash2, UserRound } from 'lucide-react'
import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { createNote, deleteNote, listNotes } from '../api/notes'
import { errorMessage } from '../api/client'
import type { Note } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { AppShell } from '../components/AppShell'
import { Button, DangerButton, ErrorText, Input, Spinner } from '../components/ui'

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
    if (!value) {
      setError('请输入笔记标题')
      return
    }
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
    setError('')
    try {
      await deleteNote(note.id)
      load()
    } catch (err) {
      setError(errorMessage(err, '删除笔记失败'))
    }
  }

  return (
    <AppShell>
      <div className="mx-auto w-[min(100%-24px,1180px)] py-8 md:w-[min(100%-32px,1180px)]">
        <section className="mb-6 flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
          <div>
            <p className="text-sm font-bold text-teal-700">你好，{user?.username}</p>
            <h1 className="mt-2 text-3xl font-extrabold tracking-normal text-slate-950">协作笔记</h1>
            <p className="mt-3 max-w-2xl text-sm leading-7 text-slate-500">Yjs 文档同步、Hocuspocus 鉴权、Spring 持久化快照。</p>
          </div>
          <Chip variant="soft" color="success">
            ws://localhost:19082/collab/notes
          </Chip>
        </section>

        <Card className="mb-5 border border-slate-200/80 bg-white/90 shadow-sm">
          <Card.Content>
            <form onSubmit={onCreate} className="grid gap-3 sm:grid-cols-[1fr_auto]">
              <Input placeholder="新笔记标题" value={title} onChange={(e) => setTitle(e.target.value)} maxLength={200} />
              <Button type="submit" disabled={saving}>
                <Plus size={17} />
                {saving ? '创建中…' : '新建笔记'}
              </Button>
            </form>
            <ErrorText>{error}</ErrorText>
          </Card.Content>
        </Card>

        {loading ? (
          <Spinner />
        ) : notes.length === 0 ? (
          <Card className="border border-dashed border-slate-300 bg-white/70">
            <Card.Content className="items-center p-10 text-center">
              <div className="grid h-12 w-12 place-items-center rounded-2xl bg-teal-50 text-teal-700">
                <NotebookPen size={22} />
              </div>
              <p className="mt-3 font-semibold text-slate-700">暂无笔记</p>
              <p className="mt-1 text-sm text-slate-500">先创建一篇，然后邀请另一个账号进入实时协作。</p>
            </Card.Content>
          </Card>
        ) : (
          <ul className="grid gap-3">
            {notes.map((note) => (
              <li key={note.id}>
                <Card className="border border-slate-200/80 bg-white/90 shadow-sm transition hover:-translate-y-0.5 hover:border-teal-200 hover:shadow-md">
                  <Card.Header className="flex-row items-start justify-between gap-3">
                    <Link to={`/notes/${note.id}`} className="flex min-w-0 flex-1 gap-3">
                      <div className="mt-1 grid h-10 w-10 shrink-0 place-items-center rounded-xl bg-teal-50 text-teal-700">
                        <NotebookPen size={18} />
                      </div>
                      <div className="min-w-0">
                        <h2 className="truncate text-lg font-bold text-slate-950">{note.title}</h2>
                        <p className="mt-1 inline-flex items-center gap-1.5 text-sm text-slate-500">
                          <UserRound size={14} />
                          拥有者 {note.ownerUsername}
                        </p>
                      </div>
                    </Link>
                    <Chip size="sm" variant="soft" color={note.role === 'OWNER' ? 'accent' : 'default'}>
                      {note.role}
                    </Chip>
                  </Card.Header>
                  <Separator />
                  <Card.Content className="flex flex-row items-center justify-between py-3 text-xs font-medium text-slate-400">
                    <span className="inline-flex items-center gap-1.5">
                      <RadioTower size={14} />
                      更新 {note.updatedAt.slice(0, 10)}
                    </span>
                    {note.role === 'OWNER' && (
                      <DangerButton size="sm" className="min-w-0 px-3" onClick={() => onDelete(note)}>
                        <Trash2 size={14} />
                        删除
                      </DangerButton>
                    )}
                  </Card.Content>
                </Card>
              </li>
            ))}
          </ul>
        )}
      </div>
    </AppShell>
  )
}
