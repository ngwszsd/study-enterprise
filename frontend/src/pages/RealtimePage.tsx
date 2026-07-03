import { useEffect, useRef, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { getToken } from '../auth/token'
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
  const items: Array<{ name: string; desc: string; live?: boolean }> = [
    { name: 'JWT + Spring Security', desc: '登陆鉴权、无状态过滤链' },
    { name: 'MyBatis-Plus + MySQL', desc: '文章 CRUD、分页、条件查询' },
    { name: '手写 SQL', desc: '分类统计 @Select GROUP BY' },
    { name: 'Redis', desc: '文章详情缓存 + 浏览量计数' },
    { name: 'MinIO', desc: '封面图上传/删除、预签名 URL' },
    { name: 'WebSocket', desc: '实时聊天(双向)', live: wsOn },
    { name: 'SSE', desc: '新文章实时通知(服务端推送)', live: sseOn },
  ]
  return (
    <div className="rounded-xl border border-gray-200 bg-white p-4">
      <h2 className="mb-3 text-sm font-semibold text-gray-900">技术栈(全部用于本项目)</h2>
      <ul className="grid gap-2 sm:grid-cols-2">
        {items.map((it) => (
          <li key={it.name} className="flex items-start gap-2">
            <span
              className={`mt-1 h-2 w-2 shrink-0 rounded-full ${
                it.live === undefined ? 'bg-emerald-500' : it.live ? 'bg-emerald-500' : 'bg-gray-300'
              }`}
            />
            <div>
              <div className="text-sm font-medium text-gray-800">
                {it.name}
                {it.live !== undefined && (
                  <span className={`ml-2 text-xs ${it.live ? 'text-emerald-600' : 'text-gray-400'}`}>
                    {it.live ? '已连接' : '未连接'}
                  </span>
                )}
              </div>
              <div className="text-xs text-gray-500">{it.desc}</div>
            </div>
          </li>
        ))}
      </ul>
    </div>
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
    <div className="mx-auto max-w-4xl px-4 py-6">
      <header className="mb-4 flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">实时 · WebSocket + SSE</h1>
        <Link to="/articles" className="text-sm text-indigo-600 hover:underline">
          ← 返回文章
        </Link>
      </header>

      <div className="mb-4">
        <TechPanel wsOn={wsConnected} sseOn={sseConnected} />
      </div>

      <div className="grid gap-4 md:grid-cols-2">
        {/* WebSocket 聊天 */}
        <section className="flex flex-col rounded-xl border border-gray-200 bg-white p-4">
          <div className="mb-2 flex items-center justify-between">
            <h2 className="font-semibold text-gray-900">聊天室(WebSocket)</h2>
            <span className={`text-xs ${wsConnected ? 'text-emerald-600' : 'text-gray-400'}`}>
              {wsConnected ? '● 在线' : '○ 离线'}
            </span>
          </div>
          <div className="mb-3 h-64 space-y-1 overflow-y-auto rounded-md bg-gray-50 p-2 text-sm">
            {messages.length === 0 ? (
              <p className="p-4 text-center text-gray-400">还没有消息,发一条试试</p>
            ) : (
              messages.map((m, i) => (
                <div key={i} className={m.from === 'system' ? 'text-gray-400' : ''}>
                  <span className="text-xs text-gray-400">{m.time} </span>
                  <span className="font-medium">{m.from === 'system' ? '' : `${m.from}:`}</span> {m.text}
                </div>
              ))
            )}
          </div>
          <form onSubmit={send} className="flex gap-2">
            <Input placeholder="说点什么…" value={input} onChange={(e) => setInput(e.target.value)} />
            <Button type="submit" disabled={!wsConnected}>
              发送
            </Button>
          </form>
          <p className="mt-2 text-xs text-gray-400">提示:另开一个标签页登陆,两边即可实时对话。</p>
        </section>

        {/* SSE 通知 */}
        <section className="flex flex-col rounded-xl border border-gray-200 bg-white p-4">
          <div className="mb-2 flex items-center justify-between">
            <h2 className="font-semibold text-gray-900">实时通知(SSE)</h2>
            <span className={`text-xs ${sseConnected ? 'text-emerald-600' : 'text-gray-400'}`}>
              {sseConnected ? '● 已连接' : '○ 连接中'}
            </span>
          </div>
          <div className="h-64 space-y-2 overflow-y-auto rounded-md bg-gray-50 p-2 text-sm">
            {notices.length === 0 ? (
              <p className="p-4 text-center text-gray-400">去发一篇文章,这里会实时收到推送</p>
            ) : (
              notices.map((n, i) => (
                <div key={i} className="rounded-md border border-gray-100 bg-white p-2">
                  <div className="text-xs text-gray-400">{n.at}</div>
                  <div>
                    📝 新文章:《{n.title}》 by {n.author}
                  </div>
                </div>
              ))
            )}
          </div>
          <p className="mt-2 text-xs text-gray-400">你({user?.username})或他人一发文章,这里立刻收到。</p>
        </section>
      </div>
    </div>
  )
}
