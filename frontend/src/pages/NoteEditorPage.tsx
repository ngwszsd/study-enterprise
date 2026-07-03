import { HocuspocusProvider } from '@hocuspocus/provider'
import { useEffect, useMemo, useRef, useState, type ChangeEvent, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import * as Y from 'yjs'
import {
  addNoteMember,
  createCollabToken,
  getNote,
  listNoteMembers,
  removeNoteMember,
  updateNote,
} from '../api/notes'
import { errorMessage } from '../api/client'
import type { Note, NoteMember } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { Button, ErrorText, GhostButton, Input, Spinner } from '../components/ui'

const COLORS = ['#2563eb', '#059669', '#dc2626', '#7c3aed', '#ea580c', '#0891b2']

export default function NoteEditorPage() {
  const { id } = useParams()
  const noteId = Number(id)
  const navigate = useNavigate()
  const { user } = useAuth()
  const ydoc = useMemo(() => new Y.Doc(), [noteId])
  const ytext = useMemo(() => ydoc.getText('content'), [ydoc])
  const providerRef = useRef<HocuspocusProvider | null>(null)

  const [note, setNote] = useState<Note | null>(null)
  const [title, setTitle] = useState('')
  const [content, setContent] = useState('')
  const [members, setMembers] = useState<NoteMember[]>([])
  const [onlineUsers, setOnlineUsers] = useState<string[]>([])
  const [status, setStatus] = useState('连接中')
  const [memberUserId, setMemberUserId] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)
  const applyingRemote = useRef(false)

  useEffect(() => {
    let active = true
    Promise.all([getNote(noteId), listNoteMembers(noteId)])
      .then(([noteRes, membersRes]) => {
        if (!active) return
        setNote(noteRes.data)
        setTitle(noteRes.data.title)
        setMembers(membersRes.data)
      })
      .catch((e) => active && setError(errorMessage(e)))
      .finally(() => active && setLoading(false))
    return () => {
      active = false
    }
  }, [noteId])

  useEffect(() => {
    if (!user || !note || note.role === 'VIEWER') return
    let provider: HocuspocusProvider | null = null
    let active = true

    createCollabToken(noteId)
      .then(({ data }) => {
        if (!active) return
        provider = new HocuspocusProvider({
          url: data.url,
          name: data.docName,
          token: data.token,
          document: ydoc,
          onStatus: ({ status }) => setStatus(status),
          onAuthenticationFailed: ({ reason }) => setError(`协作鉴权失败：${reason}`),
          onSynced: ({ state }) => state && setStatus('已同步'),
        })
        providerRef.current = provider
        provider.awareness?.setLocalStateField('user', {
          id: user.id,
          name: user.username,
          color: COLORS[user.id % COLORS.length],
        })
        provider.awareness?.on('change', () => {
          const users = Array.from(provider?.awareness?.getStates().values() ?? [])
            .map((state) => state.user?.name)
            .filter(Boolean)
          setOnlineUsers(Array.from(new Set(users)))
        })
      })
      .catch((e) => setError(errorMessage(e, '获取协作 token 失败')))

    return () => {
      active = false
      provider?.destroy()
      providerRef.current = null
      ydoc.destroy()
    }
  }, [noteId, user, ydoc, note?.role])

  useEffect(() => {
    const syncFromDoc = () => {
      applyingRemote.current = true
      setContent(ytext.toString())
      queueMicrotask(() => {
        applyingRemote.current = false
      })
    }
    ytext.observe(syncFromDoc)
    syncFromDoc()
    return () => ytext.unobserve(syncFromDoc)
  }, [ytext])

  const onContentChange = (e: ChangeEvent<HTMLTextAreaElement>) => {
    const next = e.target.value
    setContent(next)
    if (applyingRemote.current) return
    ydoc.transact(() => {
      ytext.delete(0, ytext.length)
      ytext.insert(0, next)
    })
  }

  const onSaveTitle = async (e: FormEvent) => {
    e.preventDefault()
    setError('')
    try {
      const { data } = await updateNote(noteId, { title })
      setNote(data)
    } catch (err) {
      setError(errorMessage(err, '保存标题失败'))
    }
  }

  const onAddMember = async (e: FormEvent) => {
    e.preventDefault()
    const userId = Number(memberUserId)
    if (!userId) return
    setError('')
    try {
      await addNoteMember(noteId, { userId, role: 'EDITOR' })
      setMemberUserId('')
      const { data } = await listNoteMembers(noteId)
      setMembers(data)
    } catch (err) {
      setError(errorMessage(err, '添加成员失败'))
    }
  }

  const onRemoveMember = async (member: NoteMember) => {
    await removeNoteMember(noteId, member.userId)
    const { data } = await listNoteMembers(noteId)
    setMembers(data)
  }

  if (loading) return <Spinner />

  if (!note) {
    return (
      <div className="mx-auto max-w-3xl px-4 py-6">
        <ErrorText>{error || '笔记不存在'}</ErrorText>
        <GhostButton onClick={() => navigate('/notes')}>返回</GhostButton>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-6xl px-4 py-6">
      <header className="mb-4 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">多人协作笔记</h1>
          <p className="mt-1 text-sm text-gray-500">
            {status} · {onlineUsers.length > 0 ? `在线：${onlineUsers.join('、')}` : '等待协作者'}
          </p>
        </div>
        <Link to="/notes" className="text-sm text-indigo-600 hover:underline">
          返回笔记
        </Link>
      </header>

      <ErrorText>{error}</ErrorText>

      <div className="grid gap-4 lg:grid-cols-[1fr_280px]">
        <section className="space-y-3">
          <form onSubmit={onSaveTitle} className="flex gap-2">
            <Input value={title} onChange={(e) => setTitle(e.target.value)} maxLength={200} />
            <Button type="submit" disabled={note.role !== 'OWNER'}>
              保存标题
            </Button>
          </form>

          <textarea
            value={content}
            onChange={onContentChange}
            readOnly={note.role === 'VIEWER'}
            className="min-h-[560px] w-full resize-y rounded-lg border border-gray-300 bg-white p-4 font-mono text-sm leading-6 text-gray-800 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
            placeholder={
              note.role === 'VIEWER'
                ? 'VIEWER 成员暂未接入只读协作流。'
                : '在这里输入内容。另开一个浏览器窗口进入同一篇笔记，可以看到实时同步。'
            }
          />
        </section>

        <aside className="space-y-4">
          <section className="rounded-lg border border-gray-200 bg-white p-4">
            <h2 className="mb-3 text-sm font-semibold text-gray-900">成员</h2>
            <ul className="space-y-2 text-sm">
              {members.map((member) => (
                <li key={member.userId} className="flex items-center justify-between gap-2">
                  <span className="min-w-0 truncate">
                    {member.username ?? `用户 ${member.userId}`}
                    <span className="ml-2 text-xs text-gray-400">{member.role}</span>
                  </span>
                  {note.role === 'OWNER' && member.role !== 'OWNER' && (
                    <button className="text-xs text-red-500" onClick={() => onRemoveMember(member)}>
                      移除
                    </button>
                  )}
                </li>
              ))}
            </ul>
          </section>

          {note.role === 'OWNER' && (
            <form onSubmit={onAddMember} className="rounded-lg border border-gray-200 bg-white p-4">
              <h2 className="mb-3 text-sm font-semibold text-gray-900">添加编辑者</h2>
              <div className="flex gap-2">
                <Input
                  type="number"
                  placeholder="用户 ID"
                  value={memberUserId}
                  onChange={(e) => setMemberUserId(e.target.value)}
                />
                <Button type="submit">添加</Button>
              </div>
            </form>
          )}
        </aside>
      </div>
    </div>
  )
}
