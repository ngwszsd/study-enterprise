import { useEffect, useRef, useState, type FormEvent } from 'react'
import { Card, Chip, Separator } from '@heroui/react'
import { ArrowLeft, BellRing, Cable, Database, RadioTower, Send, ServerCog, ShieldCheck, Wifi } from 'lucide-react'
import { Link } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { getToken } from '../auth/token'
import { AppShell } from '../components/AppShell'
import { Button, Input } from '../components/ui'

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:18080'
const WS_BASE = API_BASE.replace(/^http/, 'ws')

interface ChatMsg {
  from: string
  text: string
  time: string
}

interface Notice {
  title: string
  author: string
  at: string
}

/** 技术栈看板:体现本项目用到的全部技术;WS/SSE 显示实时连接状态。 */
function TechPanel({ wsOn, sseOn }: { wsOn: boolean; sseOn: boolean }) {
  const items: Array<{ name: string; desc: string; icon: typeof ShieldCheck; live?: boolean }> = [
    { name: 'JWT + Spring Security', desc: '登录鉴权、无状态过滤链', icon: ShieldCheck },
    { name: 'MyBatis-Plus + MySQL', desc: '文章 CRUD、分页、条件查询', icon: Database },
    { name: '手写 SQL', desc: '分类统计 @Select GROUP BY', icon: ServerCog },
    { name: 'Redis', desc: '文章详情缓存 + 浏览量计数', icon: Database },
    { name: 'MinIO', desc: '封面图上传/删除、预签名 URL', icon: ServerCog },
    { name: 'WebSocket', desc: '实时聊天(双向)', icon: Cable, live: wsOn },
    { name: 'SSE', desc: '新文章实时通知(服务端推送)', icon: RadioTower, live: sseOn },
  ]
  return (
    <Card className="border border-slate-200/80 bg-white/90 shadow-sm">
      <Card.Header>
        <h2 className="text-sm font-bold text-slate-900">技术栈(全部用于本项目)</h2>
      </Card.Header>
      <Separator />
      <Card.Content>
      <ul className="grid gap-2 sm:grid-cols-2">
        {items.map((it) => (
          <li key={it.name} className="flex items-start gap-3 rounded-xl border border-slate-200 bg-slate-50 p-3">
            <span className="grid h-8 w-8 shrink-0 place-items-center rounded-lg bg-teal-50 text-teal-700">
              <it.icon size={16} />
            </span>
            <div>
              <div className="text-sm font-bold text-slate-700">
                {it.name}
                {it.live !== undefined && (
                  <span className={`ml-2 text-xs ${it.live ? 'text-emerald-700' : 'text-slate-400'}`}>
                    {it.live ? '已连接' : '未连接'}
                  </span>
                )}
              </div>
              <div className="mt-1 text-xs leading-5 text-slate-500">{it.desc}</div>
            </div>
          </li>
        ))}
      </ul>
      </Card.Content>
    </Card>
  )
}

export default function RealtimePage() {
  const { user } = useAuth()
  const token = getToken() ?? ''

  const [wsConnected, setWsConnected] = useState(false)
  const [messages, setMessages] = useState<ChatMsg[]>([])
  const [input, setInput] = useState('')
  const wsRef = useRef<WebSocket | null>(null)

  const [sseConnected, setSseConnected] = useState(false)
  const [notices, setNotices] = useState<Notice[]>([])

  // WebSocket 聊天
  useEffect(() => {
    const ws = new WebSocket(`${WS_BASE}/ws/chat?token=${encodeURIComponent(token)}`)
    wsRef.current = ws
    ws.onopen = () => setWsConnected(true)
    ws.onclose = () => setWsConnected(false)
    ws.onmessage = (e) => {
      try {
        setMessages((prev) => [...prev, JSON.parse(e.data) as ChatMsg])
      } catch {
        /* 忽略非 JSON */
      }
    }
    return () => ws.close()
  }, [token])

  // SSE 通知
  useEffect(() => {
    const es = new EventSource(`${API_BASE}/api/sse/notifications?token=${encodeURIComponent(token)}`)
    es.addEventListener('connected', () => setSseConnected(true))
    es.addEventListener('article-created', (e) => {
      try {
        const data = JSON.parse((e as MessageEvent).data)
        setNotices((prev) => [{ title: data.title, author: data.author, at: new Date().toLocaleTimeString() }, ...prev])
      } catch {
        /* 忽略 */
      }
    })
    es.onerror = () => setSseConnected(false)
    return () => es.close()
  }, [token])

  const send = (e: FormEvent) => {
    e.preventDefault()
    const text = input.trim()
    if (text && wsRef.current?.readyState === WebSocket.OPEN) {
      wsRef.current.send(text)
      setInput('')
    }
  }

  return (
    <AppShell>
      <div className="mx-auto w-[min(100%-24px,1180px)] py-8 md:w-[min(100%-32px,1180px)]">
        <header className="mb-5 flex flex-col gap-3 md:flex-row md:items-end md:justify-between">
          <div>
            <div className="flex gap-2">
              <Chip size="sm" variant="soft" color={wsConnected ? 'success' : 'default'}>WS</Chip>
              <Chip size="sm" variant="soft" color={sseConnected ? 'success' : 'default'}>SSE</Chip>
            </div>
            <h1 className="mt-3 text-3xl font-extrabold tracking-normal text-slate-950">实时链路</h1>
            <p className="mt-2 max-w-2xl text-sm leading-7 text-slate-500">WebSocket 双向聊天，SSE 服务端通知。</p>
          </div>
          <Link to="/articles" className="inline-flex items-center gap-2 text-sm font-semibold text-teal-700 hover:underline">
            <ArrowLeft size={16} />
            返回文章
          </Link>
        </header>

      <div className="mb-4">
        <TechPanel wsOn={wsConnected} sseOn={sseConnected} />
      </div>

      <div className="grid gap-4 md:grid-cols-2">
        {/* WebSocket 聊天 */}
        <Card className="border border-slate-200/80 bg-white/90 shadow-sm">
          <Card.Header className="flex-row justify-between">
            <h2 className="inline-flex items-center gap-2 font-bold text-slate-900">
              <Wifi size={17} />
              聊天室(WebSocket)
            </h2>
            <Chip size="sm" variant="soft" color={wsConnected ? 'success' : 'default'}>{wsConnected ? '在线' : '离线'}</Chip>
          </Card.Header>
          <Card.Content>
          <div className="mb-3 h-72 space-y-2 overflow-y-auto rounded-2xl border border-slate-200 bg-slate-50 p-3 text-sm">
            {messages.length === 0 ? (
              <p className="p-6 text-center text-slate-400">还没有消息，发一条试试</p>
            ) : (
              messages.map((m, i) => (
                <div
                  key={i}
                  className={`rounded-xl px-3 py-2 ${
                    m.from === 'system'
                      ? 'bg-transparent text-slate-400'
                      : 'bg-white text-slate-700 shadow-sm'
                  }`}
                >
                  <span className="text-xs text-slate-400">{m.time} </span>
                  <span className="font-bold">{m.from === 'system' ? '' : `${m.from}:`}</span> {m.text}
                </div>
              ))
            )}
          </div>
          <form onSubmit={send} className="grid gap-2 sm:grid-cols-[1fr_auto]">
            <Input placeholder="说点什么…" value={input} onChange={(e) => setInput(e.target.value)} />
            <Button type="submit" disabled={!wsConnected}>
              <Send size={16} />
              发送
            </Button>
          </form>
          <p className="mt-2 text-xs text-slate-400">提示：另开一个标签页登录，两边即可实时对话。</p>
          </Card.Content>
        </Card>

        {/* SSE 通知 */}
        <Card className="border border-slate-200/80 bg-white/90 shadow-sm">
          <Card.Header className="flex-row justify-between">
            <h2 className="inline-flex items-center gap-2 font-bold text-slate-900">
              <BellRing size={17} />
              实时通知(SSE)
            </h2>
            <Chip size="sm" variant="soft" color={sseConnected ? 'success' : 'default'}>{sseConnected ? '已连接' : '连接中'}</Chip>
          </Card.Header>
          <Card.Content>
          <div className="h-72 space-y-2 overflow-y-auto rounded-2xl border border-slate-200 bg-slate-50 p-3 text-sm">
            {notices.length === 0 ? (
              <p className="p-6 text-center text-slate-400">去发一篇文章，这里会实时收到推送</p>
            ) : (
              notices.map((n, i) => (
                <div key={i} className="rounded-xl border border-slate-200 bg-white p-3 shadow-sm">
                  <div className="text-xs text-slate-400">{n.at}</div>
                  <div className="mt-1 font-semibold text-slate-700">
                    新文章：《{n.title}》 by {n.author}
                  </div>
                </div>
              ))
            )}
          </div>
          <p className="mt-2 text-xs text-slate-400">你（{user?.username}）或他人一发文章，这里立刻收到。</p>
          </Card.Content>
        </Card>
      </div>
      </div>
    </AppShell>
  )
}
