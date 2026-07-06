import { useEffect, useState } from 'react'
import { Card, Chip, Separator } from '@heroui/react'
import DOMPurify from 'dompurify'
import { ArrowLeft, Calendar, Eye, PenLine, Trash2, UserRound } from 'lucide-react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { deleteArticle, getArticle } from '../api/articles'
import { errorMessage } from '../api/client'
import type { Article } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { AppShell } from '../components/AppShell'
import { Button, DangerButton, Spinner } from '../components/ui'

function renderArticleHtml(content: string) {
  const html = /<\/?[a-z][\s\S]*>/i.test(content) ? content : `<p>${content.replace(/\n/g, '<br>')}</p>`
  return DOMPurify.sanitize(html, {
    ALLOWED_TAGS: [
      'p',
      'br',
      'strong',
      'em',
      'u',
      's',
      'mark',
      'h1',
      'h2',
      'h3',
      'ul',
      'ol',
      'li',
      'blockquote',
      'a',
      'code',
      'pre',
      'hr',
      'label',
      'input',
      'span',
      'div',
    ],
    ALLOWED_ATTR: ['href', 'target', 'rel', 'class', 'style', 'data-type', 'data-checked', 'type', 'checked', 'disabled'],
  })
}

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
  if (error) {
    return (
      <AppShell>
        <div className="mx-auto w-[min(100%-24px,1180px)] py-8 md:w-[min(100%-32px,1180px)]">
          <p className="rounded-xl border border-rose-200 bg-rose-50 px-3 py-2 text-sm leading-6 text-rose-700">{error}</p>
        </div>
      </AppShell>
    )
  }
  if (!article) return null

  const isOwner = user?.id === article.authorId
  const safeContent = renderArticleHtml(article.content)

  return (
    <AppShell>
      <div className="mx-auto w-[min(100%-24px,1180px)] py-8 md:w-[min(100%-32px,1180px)]">
        <div className="mx-auto max-w-4xl">
      <Link to="/articles" className="inline-flex items-center gap-2 text-sm font-semibold text-teal-700 hover:underline">
        <ArrowLeft size={16} />
        返回列表
      </Link>

      <div className="mt-4 flex items-start justify-between gap-4">
        <h1 className="text-3xl font-extrabold tracking-normal text-slate-950">{article.title}</h1>
        {isOwner && (
          <div className="flex shrink-0 gap-2">
            <Button onClick={() => navigate(`/articles/${article.id}/edit`)}>
              <PenLine size={17} />
              编辑
            </Button>
            <DangerButton onClick={onDelete}>
              <Trash2 size={17} />
              删除
            </DangerButton>
          </div>
        )}
      </div>

      <div className="mt-4 flex flex-wrap items-center gap-3 text-sm font-medium text-slate-500">
        <span className="inline-flex items-center gap-1.5">
          <UserRound size={15} />
          {article.authorUsername}
        </span>
        {article.category && (
          <Chip size="sm" variant="soft" color="default">
            {article.category}
          </Chip>
        )}
        <span className="inline-flex items-center gap-1.5">
          <Eye size={15} />
          浏览 {article.viewCount}
        </span>
        <span className="inline-flex items-center gap-1.5">
          <Calendar size={15} />
          {article.createdAt.slice(0, 19).replace('T', ' ')}
        </span>
      </div>

      {article.coverImageUrl && (
        <img
          src={article.coverImageUrl}
          alt="封面"
          className="mt-6 max-h-96 w-full rounded-2xl border border-slate-200 object-cover shadow-sm"
        />
      )}

      <Card className="mt-6 border border-slate-200/80 bg-white/90 shadow-sm">
        <Card.Header>
          <h2 className="text-sm font-bold text-slate-900">正文</h2>
        </Card.Header>
        <Separator />
        <Card.Content
          className="px-5 py-5 text-[0.98rem] leading-8 text-slate-700 [&_a]:text-teal-700 [&_a]:underline [&_blockquote]:rounded-xl [&_blockquote]:border-l-4 [&_blockquote]:border-teal-200 [&_blockquote]:bg-teal-50 [&_blockquote]:px-4 [&_blockquote]:py-2 [&_code]:rounded [&_code]:bg-slate-100 [&_code]:px-1.5 [&_code]:py-0.5 [&_code]:text-[0.85em] [&_h1]:text-2xl [&_h1]:font-bold [&_h2]:text-xl [&_h2]:font-bold [&_h3]:text-lg [&_h3]:font-bold [&_hr]:my-5 [&_hr]:border-slate-200 [&_li]:my-1 [&_mark]:rounded [&_mark]:bg-amber-100 [&_mark]:px-0.5 [&_ol]:list-decimal [&_ol]:pl-6 [&_p]:my-2 [&_pre]:overflow-auto [&_pre]:rounded-xl [&_pre]:bg-slate-950 [&_pre]:p-4 [&_pre_code]:bg-transparent [&_pre_code]:p-0 [&_pre_code]:text-slate-100 [&_ul:not([data-type='taskList'])]:list-disc [&_ul]:pl-6 [&_ul[data-type='taskList']_input]:pointer-events-none [&_ul[data-type='taskList']_input]:mt-1 [&_ul[data-type='taskList']_input]:accent-teal-600 [&_ul[data-type='taskList']_li>div]:min-w-0 [&_ul[data-type='taskList']_li>label]:mt-1 [&_ul[data-type='taskList']_li]:flex [&_ul[data-type='taskList']_li]:gap-2 [&_ul[data-type='taskList']]:list-none [&_ul[data-type='taskList']]:pl-0"
          dangerouslySetInnerHTML={{ __html: safeContent }}
        />
      </Card>
        </div>
      </div>
    </AppShell>
  )
}
