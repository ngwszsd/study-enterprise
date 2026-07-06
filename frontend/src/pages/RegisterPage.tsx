import { Card, Chip } from '@heroui/react'
import { ArrowRight, BookOpenText, LockKeyhole, Network, RadioTower } from 'lucide-react'
import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import * as authApi from '../api/auth'
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
  { icon: BookOpenText, title: '注册即进入业务', desc: '账号创建后自动登录，直接进入文章工作台' },
  { icon: RadioTower, title: '双实时能力', desc: 'WebSocket 负责双向聊天，SSE 负责通知流' },
  { icon: LockKeyhole, title: '协作权限模型', desc: 'OWNER、EDITOR、VIEWER 与短期协作 token' },
]

export default function RegisterPage() {
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
      await authApi.register(username, password)
      await login(username, password) // 注册成功后自动登录
      navigate('/articles')
    } catch (err) {
      setError(errorMessage(err, '注册失败'))
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
            <p className="text-sm font-semibold text-amber-200">MVP Account</p>
            <h2 className="mt-3 text-4xl font-extrabold leading-tight">先有用户，再看完整权限链路。</h2>
            <p className="mt-5 text-sm leading-7 text-slate-300">
              账号会贯穿普通 REST API、WebSocket/SSE 握手和协作笔记短期 token，是理解全链路鉴权的入口。
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
            <Chip size="sm" variant="soft" color="accent">
              新账号
            </Chip>
            <div>
              <h2 className="text-3xl font-extrabold tracking-normal text-slate-950">注册学习账号</h2>
              <p className="mt-3 text-sm leading-7 text-slate-500">注册后会自动登录，直接进入全链路页面。</p>
            </div>
          </Card.Header>
          <Card.Content className="px-6 pb-8 pt-2 sm:px-10 sm:pb-10">
            <form onSubmit={onSubmit} className="space-y-5">
              <AuthField label="用户名（3-50 字符）">
                <Input
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  autoFocus
                  autoComplete="username"
                  placeholder="bob_demo"
                  className="w-full"
                />
              </AuthField>
              <AuthField label="密码（至少 6 位）">
                <Input
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  autoComplete="new-password"
                  placeholder="secret123"
                  className="w-full"
                />
              </AuthField>
              <ErrorText>{error}</ErrorText>
              <Button type="submit" className="h-12 w-full text-base" disabled={loading}>
                {loading ? '注册中…' : '创建账号'}
                {!loading && <ArrowRight size={18} />}
              </Button>
              <p className="text-center text-sm text-slate-500">
                已有账号？
                <Link to="/login" className="ml-1 font-semibold text-teal-700 hover:underline">
                  去登录
                </Link>
              </p>
            </form>
          </Card.Content>
        </Card>
      </section>
    </main>
  )
}
