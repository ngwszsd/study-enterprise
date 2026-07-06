import { Card, Chip } from '@heroui/react'
import { ArrowRight, BookOpenText, LockKeyhole, Network, RadioTower } from 'lucide-react'
import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { errorMessage } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { Button, ErrorText, Input } from '../components/ui'

function AuthField({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="space-y-2">
      <label className="block text-sm font-semibold text-slate-700">{label}</label>
      {children}
    </div>
  )
}

const highlights = [
  { icon: BookOpenText, title: '文章业务链路', desc: 'CRUD、分页搜索、Redis 浏览量、MinIO 封面' },
  { icon: RadioTower, title: '实时链路', desc: 'WebSocket 聊天、SSE 通知、Spring 事件' },
  { icon: LockKeyhole, title: '协作鉴权', desc: 'JWT 登录、短期 collab token、Hocuspocus' },
]

export default function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await login(username, password)
      navigate('/articles')
    } catch (err) {
      setError(errorMessage(err, '登录失败'))
    } finally {
      setLoading(false)
    }
  }

  return (
    <main className="grid min-h-screen place-items-center px-4 py-8">
      <section className="grid w-full max-w-6xl overflow-hidden rounded-3xl border border-slate-200 bg-white/95 shadow-2xl shadow-slate-900/10 lg:grid-cols-[1.02fr_0.98fr]">
        <div className="hidden border-r border-slate-800 bg-slate-950 p-10 text-white lg:block">
          <div className="flex items-center gap-3">
            <div className="grid h-11 w-11 place-items-center rounded-xl bg-white/10">
              <Network size={20} />
            </div>
            <div>
              <div className="text-sm font-semibold text-teal-200">study-enterprise</div>
              <h1 className="text-2xl font-bold">全栈链路学习台</h1>
            </div>
          </div>

          <div className="mt-16 max-w-md">
            <p className="text-sm font-semibold text-amber-200">Java / Kotlin / React / Yjs</p>
            <h2 className="mt-3 text-4xl font-extrabold leading-tight">从登录开始串起完整业务链路。</h2>
            <p className="mt-5 text-sm leading-7 text-slate-300">
              这里不是展示页，登录后直接进入文章、实时消息和多人协作笔记。适合对照 Java 与 Kotlin 后端实现。
            </p>
          </div>

          <div className="mt-12 grid gap-3">
            {highlights.map((item) => {
              const Icon = item.icon
              return (
                <div key={item.title} className="rounded-2xl border border-white/10 bg-white/[0.06] p-4">
                  <div className="flex items-start gap-3">
                    <div className="grid h-9 w-9 shrink-0 place-items-center rounded-lg bg-teal-600">
                      <Icon size={17} />
                    </div>
                    <div>
                      <div className="font-semibold">{item.title}</div>
                      <div className="mt-1 text-sm leading-6 text-slate-300">{item.desc}</div>
                    </div>
                  </div>
                </div>
              )
            })}
          </div>
        </div>

        <Card className="m-0 rounded-none border-0 bg-transparent p-0 shadow-none">
          <Card.Header className="flex-col items-start gap-4 px-6 pb-3 pt-8 sm:px-10 sm:pt-10">
            <Chip size="sm" variant="soft" color="success">
              Fullstack Lab
            </Chip>
            <div>
              <h2 className="text-3xl font-extrabold tracking-normal text-slate-950">登录学习工作台</h2>
              <p className="mt-3 text-sm leading-7 text-slate-500">进入文章、实时消息和多人协作笔记链路。</p>
            </div>
          </Card.Header>
          <Card.Content className="px-6 pb-8 pt-2 sm:px-10 sm:pb-10">
            <form onSubmit={onSubmit} className="space-y-5">
              <AuthField label="用户名">
                <Input
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  autoFocus
                  autoComplete="username"
                  placeholder="alice_demo"
                  className="w-full"
                />
              </AuthField>
              <AuthField label="密码">
                <Input
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  autoComplete="current-password"
                  placeholder="secret123"
                  className="w-full"
                />
              </AuthField>
              <ErrorText>{error}</ErrorText>
              <Button type="submit" className="h-12 w-full text-base" disabled={loading}>
                {loading ? '登录中…' : '登录'}
                {!loading && <ArrowRight size={18} />}
              </Button>
              <p className="text-center text-sm text-slate-500">
                没有账号？
                <Link to="/register" className="ml-1 font-semibold text-teal-700 hover:underline">
                  去注册
                </Link>
              </p>
            </form>
          </Card.Content>
        </Card>
      </section>
    </main>
  )
}
