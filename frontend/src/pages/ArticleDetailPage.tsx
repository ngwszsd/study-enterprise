import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { deleteArticle, getArticle } from '../api/articles'
import { errorMessage } from '../api/client'
import type { Article } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { Button, GhostButton, Spinner } from '../components/ui'

export default function ArticleDetailPage() {
  const { id } = useParams()
  const articleId = Number(id)
  const { user } = useAuth()
  const navigate = useNavigate()
  const [article, setArticle] = useState<Article | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true
    setLoading(true)
    setError('')
    getArticle(articleId)
      .then((r) => active && setArticle(r.data))
      .catch((e) => active && setError(errorMessage(e)))
      .finally(() => active && setLoading(false))
    return () => {
      active = false
    }
  }, [articleId])

  const onDelete = async () => {
    if (!window.confirm('确定删除这篇文章?')) return
    try {
      await deleteArticle(articleId)
      navigate('/articles')
    } catch (e) {
      setError(errorMessage(e, '删除失败'))
    }
  }

  if (loading) return <Spinner />
  if (error) return <p className="mx-auto max-w-3xl p-8 text-red-600">{error}</p>
  if (!article) return null

  const isOwner = user?.id === article.authorId

  return (
    <div className="mx-auto max-w-3xl px-4 py-6">
      <Link to="/articles" className="text-sm text-indigo-600 hover:underline">
        ← 返回列表
      </Link>

      <div className="mt-4 flex items-start justify-between gap-4">
        <h1 className="text-3xl font-bold text-gray-900">{article.title}</h1>
        {isOwner && (
          <div className="flex shrink-0 gap-2">
            <Button onClick={() => navigate(`/articles/${article.id}/edit`)}>编辑</Button>
            <GhostButton className="text-red-600" onClick={onDelete}>
              删除
            </GhostButton>
          </div>
        )}
      </div>

      <div className="mt-2 flex flex-wrap gap-3 text-sm text-gray-400">
        <span>{article.authorUsername}</span>
        {article.category && <span>分类:{article.category}</span>}
        <span>浏览 {article.viewCount}</span>
        <span>{article.createdAt.slice(0, 19).replace('T', ' ')}</span>
      </div>

      {article.coverImageUrl && (
        <img
          src={article.coverImageUrl}
          alt="封面"
          className="mt-4 max-h-80 w-full rounded-lg object-cover"
        />
      )}

      <div className="mt-6 whitespace-pre-wrap leading-relaxed text-gray-800">{article.content}</div>
    </div>
  )
}
