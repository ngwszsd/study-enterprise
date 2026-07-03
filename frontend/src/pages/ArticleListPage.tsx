import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { getStats, listArticles } from '../api/articles'
import { errorMessage } from '../api/client'
import type { Article, CategoryCount, PageResponse } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { Button, GhostButton, Input, Spinner } from '../components/ui'

const SIZE = 5

export default function ArticleListPage() {
  const { user, logout } = useAuth()
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

  const onLogout = () => {
    logout()
    navigate('/login')
  }

  return (
    <div className="mx-auto max-w-3xl px-4 py-6">
      <header className="mb-6 flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">文章</h1>
        <div className="flex items-center gap-3 text-sm">
          <span className="text-gray-500">你好,{user?.username}</span>
          <Link to="/realtime" className="text-indigo-600 hover:underline">实时</Link>
          <Button onClick={() => navigate('/articles/new')}>写文章</Button>
          <GhostButton onClick={onLogout}>退出</GhostButton>
        </div>
      </header>

      <form onSubmit={onSearch} className="mb-4 flex gap-2">
        <Input placeholder="按标题搜索…" value={input} onChange={(e) => setInput(e.target.value)} />
        <Button type="submit">搜索</Button>
      </form>

      {stats.length > 0 && (
        <div className="mb-4 flex flex-wrap gap-2 text-xs">
          {stats.map((s) => (
            <span key={s.category} className="rounded-full bg-gray-100 px-2.5 py-1 text-gray-600">
              {s.category} · {s.count}
            </span>
          ))}
        </div>
      )}

      {loading ? (
        <Spinner />
      ) : error ? (
        <p className="rounded-md bg-red-50 p-4 text-sm text-red-600">{error}</p>
      ) : !data || data.content.length === 0 ? (
        <p className="p-8 text-center text-gray-400">暂无文章</p>
      ) : (
        <ul className="space-y-3">
          {data.content.map((a) => (
            <li key={a.id}>
              <Link
                to={`/articles/${a.id}`}
                className="block rounded-lg border border-gray-200 bg-white p-4 transition hover:border-indigo-300 hover:shadow-sm"
              >
                <div className="flex items-start justify-between gap-3">
                  <h2 className="font-semibold text-gray-900">{a.title}</h2>
                  {a.category && (
                    <span className="shrink-0 rounded-full bg-indigo-50 px-2 py-0.5 text-xs text-indigo-600">
                      {a.category}
                    </span>
                  )}
                </div>
                <p className="mt-1 line-clamp-2 text-sm text-gray-500">{a.content}</p>
                <div className="mt-2 flex gap-3 text-xs text-gray-400">
                  <span>{a.authorUsername}</span>
                  <span>浏览 {a.viewCount}</span>
                  <span>{a.createdAt.slice(0, 10)}</span>
                </div>
              </Link>
            </li>
          ))}
        </ul>
      )}

      {data && data.totalPages > 1 && (
        <div className="mt-6 flex items-center justify-center gap-4 text-sm">
          <GhostButton disabled={page <= 0} onClick={() => setPage((p) => p - 1)}>
            上一页
          </GhostButton>
          <span className="text-gray-500">
            {page + 1} / {data.totalPages}
          </span>
          <GhostButton disabled={page >= data.totalPages - 1} onClick={() => setPage((p) => p + 1)}>
            下一页
          </GhostButton>
        </div>
      )}
    </div>
  )
}
