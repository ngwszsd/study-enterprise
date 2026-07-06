import { HocuspocusProvider } from '@hocuspocus/provider'
import { Card, Chip, ComboBox, ListBox, Separator, Spinner as HeroSpinner } from '@heroui/react'
import { ArrowLeft, Circle, Plus, Save, Trash2, UserPlus, UsersRound } from 'lucide-react'
import { useCallback, useEffect, useMemo, useRef, useState, type FormEvent, type Key } from 'react'
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
import { searchUsers } from '../api/users'
import type { Note, NoteMember, UserResponse } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { AppShell } from '../components/AppShell'
import { RichTextEditor, type RichTextEditorHandle } from '../components/RichTextEditor'
import { Button, DangerButton, ErrorText, GhostButton, Input, Spinner } from '../components/ui'

const COLORS = ['#2563eb', '#059669', '#dc2626', '#7c3aed', '#ea580c', '#0891b2']
const RICH_NOTE_FIELD = 'rich-content'
const LEGACY_NOTE_FIELD = 'content'

function connectionTone(status: string) {
  return status === '已同步' || status === 'connected'
    ? { dot: 'bg-emerald-500', text: 'text-emerald-700', label: '已连接' }
    : status === 'disconnected'
      ? { dot: 'bg-rose-500', text: 'text-rose-700', label: '已断开' }
      : { dot: 'bg-amber-500', text: 'text-amber-700', label: status }
}

function escapeHtml(value: string) {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

function plainTextToHtml(value: string) {
  return value
    .split(/\n{2,}/)
    .map((paragraph) => `<p>${escapeHtml(paragraph).replace(/\n/g, '<br>')}</p>`)
    .join('')
}

export default function NoteEditorPage() {
  const { id } = useParams()
  const noteId = Number(id)
  const navigate = useNavigate()
  const { user } = useAuth()
  const ydoc = useMemo(() => new Y.Doc(), [noteId])
  const providerRef = useRef<HocuspocusProvider | null>(null)
  const richEditorRef = useRef<RichTextEditorHandle | null>(null)
  const legacyMigratedRef = useRef(false)

  const [note, setNote] = useState<Note | null>(null)
  const [title, setTitle] = useState('')
  const [members, setMembers] = useState<NoteMember[]>([])
  const [onlineUsers, setOnlineUsers] = useState<string[]>([])
  const [status, setStatus] = useState('连接中')
  const [providerSynced, setProviderSynced] = useState(false)
  const [richEditorReady, setRichEditorReady] = useState(false)
  const [collabProvider, setCollabProvider] = useState<HocuspocusProvider | null>(null)
  const [memberKeyword, setMemberKeyword] = useState('')
  const [userOptions, setUserOptions] = useState<UserResponse[]>([])
  const [selectedUser, setSelectedUser] = useState<UserResponse | null>(null)
  const [searchingUsers, setSearchingUsers] = useState(false)
  const [memberSearchError, setMemberSearchError] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)

  const memberIds = useMemo(() => new Set(members.map((member) => member.userId)), [members])
  const availableUserOptions = useMemo(
    () => userOptions.filter((option) => !memberIds.has(option.id)),
    [memberIds, userOptions],
  )
  const collabUser = useMemo(
    () =>
      user
        ? {
            id: user.id,
            name: user.username,
            color: COLORS[user.id % COLORS.length],
          }
        : null,
    [user],
  )

  useEffect(() => () => ydoc.destroy(), [ydoc])

  useEffect(() => {
    setProviderSynced(false)
    setRichEditorReady(false)
    setCollabProvider(null)
    legacyMigratedRef.current = false
    richEditorRef.current = null
  }, [noteId])

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
    if (note?.role !== 'OWNER') return
    let active = true
    const timer = window.setTimeout(() => {
      setSearchingUsers(true)
      setMemberSearchError('')
      searchUsers(memberKeyword)
        .then(({ data }) => {
          if (active) setUserOptions(data)
        })
        .catch((e) => {
          if (active) setMemberSearchError(errorMessage(e, '搜索用户失败'))
        })
        .finally(() => {
          if (active) setSearchingUsers(false)
        })
    }, 250)
    return () => {
      active = false
      window.clearTimeout(timer)
    }
  }, [memberKeyword, note?.role])

  useEffect(() => {
    if (!user || !note || note.role === 'VIEWER') return
    let provider: HocuspocusProvider | null = null
    let active = true
    setProviderSynced(false)
    setStatus('连接中')

    createCollabToken(noteId)
      .then(({ data }) => {
        if (!active) return
        let initialToken: string | null = data.token
        provider = new HocuspocusProvider({
          url: data.url,
          name: data.docName,
          token: async () => {
            if (initialToken) {
              const token = initialToken
              initialToken = null
              return token
            }
            const { data } = await createCollabToken(noteId)
            return data.token
          },
          document: ydoc,
          onStatus: ({ status }) => setStatus(status),
          onAuthenticationFailed: ({ reason }) => setError(`协作鉴权失败：${reason}`),
          onSynced: ({ state }) => {
            if (state) {
              setStatus('已同步')
              setProviderSynced(true)
            }
          },
        })
        providerRef.current = provider
        setCollabProvider(provider)
        provider.awareness?.setLocalStateField('user', collabUser)
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
      setCollabProvider(null)
    }
  }, [collabUser, noteId, user, ydoc, note?.role])

  useEffect(() => {
    if (!providerSynced || !richEditorReady || legacyMigratedRef.current) return
    const editor = richEditorRef.current
    if (!editor || !editor.isEmpty) {
      legacyMigratedRef.current = true
      return
    }
    const legacyType = ydoc.share.get(LEGACY_NOTE_FIELD)
    if (!(legacyType instanceof Y.Text)) {
      legacyMigratedRef.current = true
      return
    }
    const legacyContent = legacyType.toString().trim()
    if (!legacyContent) {
      legacyMigratedRef.current = true
      return
    }
    editor.commands.setContent(plainTextToHtml(legacyContent))
    legacyMigratedRef.current = true
  }, [providerSynced, richEditorReady, ydoc])

  const onRichEditorReady = useCallback((editor: RichTextEditorHandle) => {
    richEditorRef.current = editor
    setRichEditorReady(true)
  }, [])

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
    if (!selectedUser) return
    setError('')
    try {
      await addNoteMember(noteId, { userId: selectedUser.id, role: 'EDITOR' })
      setMemberKeyword('')
      setSelectedUser(null)
      setUserOptions([])
      const { data } = await listNoteMembers(noteId)
      setMembers(data)
    } catch (err) {
      setError(errorMessage(err, '添加成员失败'))
    }
  }

  const onMemberKeywordChange = (value: string) => {
    setMemberKeyword(value)
    if (selectedUser && value !== selectedUser.username) {
      setSelectedUser(null)
    }
  }

  const onSelectMemberUser = (key: Key | null) => {
    if (key === null) {
      setSelectedUser(null)
      return
    }
    const nextUser = availableUserOptions.find((option) => String(option.id) === String(key)) ?? null
    setSelectedUser(nextUser)
    if (nextUser) {
      setMemberKeyword(nextUser.username)
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
      <AppShell>
        <div className="mx-auto w-[min(100%-24px,1180px)] py-8 md:w-[min(100%-32px,1180px)]">
          <ErrorText>{error || '笔记不存在'}</ErrorText>
          <GhostButton onClick={() => navigate('/notes')}>返回</GhostButton>
        </div>
      </AppShell>
    )
  }

  const tone = connectionTone(status)

  return (
    <AppShell>
      <div className="mx-auto w-[min(100%-24px,1320px)] py-8 md:w-[min(100%-32px,1320px)]">
        <header className="mb-5 flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
          <div>
            <div className="flex flex-wrap items-center gap-2">
              <span className={`inline-flex items-center gap-2 rounded-full border bg-white px-3 py-1 text-xs font-bold ${tone.text}`}>
                <span className={`h-2.5 w-2.5 rounded-full ${tone.dot}`} />
                WS {tone.label}
              </span>
              <Chip size="sm" variant="soft" color="accent">{note.role}</Chip>
            </div>
            <h1 className="mt-3 text-3xl font-extrabold tracking-normal text-slate-950">多人协作笔记</h1>
            <p className="mt-2 max-w-2xl text-sm leading-7 text-slate-500">
              {onlineUsers.length > 0 ? `在线：${onlineUsers.join('、')}` : '等待协作者'}
            </p>
          </div>
          <Link to="/notes">
            <GhostButton type="button">
              <ArrowLeft size={17} />
              返回笔记
            </GhostButton>
          </Link>
        </header>

        <ErrorText>{error}</ErrorText>

        <div className="mt-4 grid gap-4 lg:grid-cols-[minmax(0,1fr)_320px]">
          <Card className="border border-slate-200/80 bg-white/90 shadow-sm">
            <Card.Content className="gap-3">
              <form onSubmit={onSaveTitle} className="grid gap-3 sm:grid-cols-[1fr_auto]">
                <Input value={title} onChange={(e) => setTitle(e.target.value)} maxLength={200} />
                <Button type="submit" disabled={note.role !== 'OWNER'}>
                  <Save size={17} />
                  保存标题
                </Button>
              </form>

              <RichTextEditor
                collaboration={{
                  document: ydoc,
                  field: RICH_NOTE_FIELD,
                  provider: collabProvider,
                  user: collabUser,
                }}
                editable={note.role !== 'VIEWER'}
                minHeightClassName="min-h-[620px]"
                onEditorReady={onRichEditorReady}
                placeholder={
                  note.role === 'VIEWER'
                    ? 'VIEWER 成员暂未接入只读协作流。'
                    : '输入富文本内容。另开一个浏览器窗口进入同一篇笔记，可以看到实时同步。'
                }
              />
            </Card.Content>
          </Card>

          <aside className="space-y-4">
            <Card className="border border-slate-200/80 bg-white/90 shadow-sm">
              <Card.Header className="flex-row items-center justify-between">
                <h2 className="inline-flex items-center gap-2 text-sm font-bold text-slate-900">
                  <UsersRound size={16} />
                  成员
                </h2>
                <Chip size="sm" variant="soft" color="default">
                  {members.length}
                </Chip>
              </Card.Header>
              <Separator />
              <Card.Content>
                <ul className="space-y-2 text-sm">
                  {members.map((member) => (
                    <li
                      key={member.userId}
                      className="flex items-center justify-between gap-2 rounded-xl border border-slate-200 bg-slate-50 px-3 py-2"
                    >
                      <span className="flex min-w-0 items-center gap-2 truncate">
                        <Circle size={9} className="shrink-0 fill-teal-600 text-teal-600" />
                        <span className="truncate font-semibold text-slate-700">
                          {member.username ?? `用户 ${member.userId}`}
                        </span>
                        <span className="text-xs text-slate-400">{member.role}</span>
                      </span>
                      {note.role === 'OWNER' && member.role !== 'OWNER' && (
                        <DangerButton size="sm" className="min-w-0 px-2" onClick={() => onRemoveMember(member)}>
                          <Trash2 size={13} />
                        </DangerButton>
                      )}
                    </li>
                  ))}
                </ul>
              </Card.Content>
            </Card>

            {note.role === 'OWNER' && (
              <Card className="border border-slate-200/80 bg-white/90 shadow-sm">
                <Card.Content>
                  <form onSubmit={onAddMember} className="space-y-3">
                    <h2 className="inline-flex items-center gap-2 text-sm font-bold text-slate-900">
                      <UserPlus size={16} />
                      添加编辑者
                    </h2>

                    <ComboBox<UserResponse>
                      aria-label="搜索用户"
                      allowsCustomValue
                      fullWidth
                      inputValue={memberKeyword}
                      menuTrigger="input"
                      selectedKey={selectedUser ? String(selectedUser.id) : null}
                      variant="primary"
                      onInputChange={onMemberKeywordChange}
                      onSelectionChange={onSelectMemberUser}
                    >
                      <ComboBox.InputGroup>
                        <Input placeholder="输入用户名搜索" />
                        <ComboBox.Trigger aria-label="展开用户列表" />
                      </ComboBox.InputGroup>
                      <ComboBox.Popover className="min-w-72">
                        {searchingUsers ? (
                          <div className="flex items-center gap-2 px-3 py-3 text-sm text-slate-500">
                            <HeroSpinner size="sm" color="accent" />
                            搜索中
                          </div>
                        ) : availableUserOptions.length > 0 ? (
                          <ListBox aria-label="用户搜索结果">
                            {availableUserOptions.map((option) => (
                              <ListBox.Item key={option.id} id={String(option.id)} textValue={option.username}>
                                <div className="flex min-w-0 flex-col">
                                  <span className="truncate text-sm font-semibold text-slate-800">
                                    {option.username}
                                  </span>
                                  <span className="text-xs text-slate-400">ID {option.id}</span>
                                </div>
                              </ListBox.Item>
                            ))}
                          </ListBox>
                        ) : (
                          <div className="px-3 py-3 text-sm text-slate-500">
                            {memberKeyword.trim() ? '没有匹配的可添加用户' : '暂无可添加用户'}
                          </div>
                        )}
                      </ComboBox.Popover>
                    </ComboBox>

                    {selectedUser && (
                      <Chip size="sm" variant="soft" color="accent">
                        已选择：{selectedUser.username} · ID {selectedUser.id}
                      </Chip>
                    )}
                    {memberSearchError && <p className="text-xs leading-5 text-rose-600">{memberSearchError}</p>}

                    <Button type="submit" fullWidth disabled={!selectedUser}>
                      <Plus size={16} />
                      添加为编辑者
                    </Button>
                  </form>
                </Card.Content>
              </Card>
            )}
          </aside>
        </div>
      </div>
    </AppShell>
  )
}
