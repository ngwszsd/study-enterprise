import { useEffect, useState, type FormEvent } from 'react'
import { Card, Chip, Separator } from '@heroui/react'
import { Eye, FileText, PenLine, Search, Sparkles } from 'lucide-react'
import { Link, useNavigate } from 'react-router-dom'
import { getStats, listArticles } from '../api/articles'
import { errorMessage } from '../api/client'
import type { Article, CategoryCount, PageResponse } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { AppShell } from '../components/AppShell'
import { Button, GhostButton, Input, Spinner } from '../components/ui'

const SIZE = 5

function plainExcerpt(content: string) {
  return content.replace(/<[^>]+>/g, ' ').replace(/\s+/g, ' ').trim()
}

export default function ArticleListPage() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const [input, setInput] = useState('')
  const [keyword, setKeyword] = useState('')
  const [page, setPage] = useState(0)
  const [data, setData] = useState<PageResponse<Article> | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [stats, setStats] = useState<CategoryCount[]>([])

  useEffect(() => {
    let active = true
    setLoading(true)
    setError('')
    listArticles(page, SIZE, keyword)
      .then((r) => active && setData(r.data))
      .catch((e) => active && setError(errorMessage(e)))
      .finally(() => active && setLoading(false))
    return () => {
      active = false
    }
  }, [page, keyword])

  useEffect(() => {
    getStats()
      .then((r) => setStats(r.data))
      .catch(() => {})
  }, [])

  const onSearch = (e: FormEvent) => {
    e.preventDefault()
    setPage(0)
    setKeyword(input.trim())
  }

  return (
    <AppShell>
      <div className="mx-auto w-[min(100%-24px,1180px)] py-8 md:w-[min(100%-32px,1180px)]">
        <section className="mb-6 flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
          <div>
            <p className="text-sm font-bold text-teal-700">你好，{user?.username}</p>
            <h1 className="mt-2 text-3xl font-extrabold tracking-normal text-slate-950">文章工作台</h1>
            <p className="mt-3 max-w-2xl text-sm leading-7 text-slate-500">MyBatis-Plus 分页搜索、Redis 浏览量、MinIO 封面链路。</p>
          </div>
          <Button onClick={() => navigate('/articles/new')}>
            <PenLine size={17} />
            写文章
          </Button>
        </section>

        <Card className="mb-5 border border-slate-200/80 bg-white/90 shadow-sm">
          <Card.Content className="gap-4">
            <form onSubmit={onSearch} className="grid gap-3 sm:grid-cols-[1fr_auto]">
              <Input placeholder="按标题搜索…" value={input} onChange={(e) => setInput(e.target.value)} />
              <Button type="submit">
                <Search size={17} />
                搜索
              </Button>
            </form>

            {stats.length > 0 && (
              <div className="flex flex-wrap gap-2">
                {stats.map((s) => (
                  <Chip key={s.category} size="sm" variant="soft" color="accent">
                    {s.category} · {s.count}
                  </Chip>
                ))}
              </div>
            )}
          </Card.Content>
        </Card>

        {loading ? (
          <Spinner />
        ) : error ? (
          <p className="rounded-xl border border-rose-200 bg-rose-50 px-3 py-2 text-sm leading-6 text-rose-700">{error}</p>
        ) : !data || data.content.length === 0 ? (
          <Card className="border border-dashed border-slate-300 bg-white/70">
            <Card.Content className="items-center p-10 text-center">
              <div className="grid h-12 w-12 place-items-center rounded-2xl bg-teal-50 text-teal-700">
                <FileText size={21} />
              </div>
              <p className="mt-3 font-semibold text-slate-700">暂无文章</p>
              <p className="mt-1 text-sm text-slate-500">先写一篇文章，验证完整后端链路。</p>
            </Card.Content>
          </Card>
        ) : (
          <ul className="grid gap-3">
            {data.content.map((a) => (
              <li key={a.id}>
                <Link to={`/articles/${a.id}`} className="block">
                  <Card className="border border-slate-200/80 bg-white/90 shadow-sm transition hover:-translate-y-0.5 hover:border-teal-200 hover:shadow-md">
                    <Card.Header className="flex-row items-start justify-between gap-3">
                      <div className="flex min-w-0 gap-3">
                        <div className="mt-1 grid h-10 w-10 shrink-0 place-items-center rounded-xl bg-teal-50 text-teal-700">
                          <FileText size={18} />
                        </div>
                        <div className="min-w-0">
                          <h2 className="truncate text-lg font-bold text-slate-950">{a.title}</h2>
                          <p className="mt-1 line-clamp-2 text-sm leading-6 text-slate-500">{plainExcerpt(a.content)}</p>
                        </div>
                      </div>
                      {a.category && (
                        <Chip size="sm" variant="soft" color="default">
                          {a.category}
                        </Chip>
                      )}
                    </Card.Header>
                    <Separator />
                    <Card.Content className="flex flex-row flex-wrap items-center gap-4 py-3 text-xs font-medium text-slate-400">
                      <span>{a.authorUsername}</span>
                      <span className="inline-flex items-center gap-1">
                        <Eye size={14} />
                        {a.viewCount}
                      </span>
                      <span>{a.createdAt.slice(0, 10)}</span>
                      {keyword && (
                        <span className="inline-flex items-center gap-1 text-teal-700">
                          <Sparkles size={14} />
                          匹配搜索
                        </span>
                      )}
                    </Card.Content>
                  </Card>
                </Link>
              </li>
            ))}
          </ul>
        )}

        {data && data.totalPages > 1 && (
          <div className="mt-6 flex items-center justify-center gap-4 text-sm">
            <GhostButton disabled={page <= 0} onClick={() => setPage((p) => p - 1)}>上一页</GhostButton>
            <span className="font-medium text-slate-500">{page + 1} / {data.totalPages}</span>
            <GhostButton disabled={page >= data.totalPages - 1} onClick={() => setPage((p) => p + 1)}>下一页</GhostButton>
          </div>
        )}
      </div>
    </AppShell>
  )
}
