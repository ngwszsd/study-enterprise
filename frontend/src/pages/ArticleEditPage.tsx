import { Card, Chip } from '@heroui/react'
import { ImagePlus, Save, X } from 'lucide-react'
import { useEffect, useState, type ChangeEvent, type FormEvent } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { createArticle, getArticle, updateArticle } from '../api/articles'
import { errorMessage } from '../api/client'
import { uploadFile } from '../api/files'
import { AppShell } from '../components/AppShell'
import { RichTextEditor } from '../components/RichTextEditor'
import { Button, ErrorText, Field, GhostButton, Input, Spinner } from '../components/ui'

export default function ArticleEditPage() {
  const { id } = useParams()
  const isEdit = Boolean(id)
  const articleId = Number(id)
  const navigate = useNavigate()

  const [title, setTitle] = useState('')
  const [content, setContent] = useState('')
  const [category, setCategory] = useState('')
  const [coverImageKey, setCoverImageKey] = useState<string | null>(null)
  const [coverUrl, setCoverUrl] = useState<string | null>(null)

  const [loading, setLoading] = useState(isEdit)
  const [saving, setSaving] = useState(false)
  const [uploading, setUploading] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!isEdit) return
    let active = true
    getArticle(articleId)
      .then((r) => {
        if (!active) return
        setTitle(r.data.title)
        setContent(r.data.content)
        setCategory(r.data.category ?? '')
        setCoverImageKey(r.data.coverImageKey)
        setCoverUrl(r.data.coverImageUrl)
      })
      .catch((e) => active && setError(errorMessage(e)))
      .finally(() => active && setLoading(false))
    return () => {
      active = false
    }
  }, [isEdit, articleId])

  const onPickCover = async (e: ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return
    setUploading(true)
    setError('')
    try {
      const { data } = await uploadFile(file)
      setCoverImageKey(data.key)
      setCoverUrl(data.url)
    } catch (err) {
      setError(errorMessage(err, '上传失败'))
    } finally {
      setUploading(false)
    }
  }

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault()
    if (!content.replace(/<[^>]+>/g, '').trim()) {
      setError('请输入正文内容')
      return
    }
    setSaving(true)
    setError('')
    const body = { title, content, category: category || null, coverImageKey }
    try {
      if (isEdit) {
        await updateArticle(articleId, body)
        navigate(`/articles/${articleId}`)
      } else {
        const { data } = await createArticle(body)
        navigate(`/articles/${data.id}`)
      }
    } catch (err) {
      setError(errorMessage(err, '保存失败'))
    } finally {
      setSaving(false)
    }
  }

  if (loading) return <Spinner />

  return (
    <AppShell>
      <div className="mx-auto w-[min(100%-24px,1180px)] py-8 md:w-[min(100%-32px,1180px)]">
        <div className="mx-auto max-w-3xl">
        <Card className="border border-slate-200/80 bg-white/90 shadow-sm">
          <Card.Header className="flex-col items-start gap-3">
            <Chip size="sm" variant="soft" color={isEdit ? 'default' : 'accent'}>
              {isEdit ? '编辑' : '新建'}
            </Chip>
            <div>
              <h1 className="text-3xl font-extrabold tracking-normal text-slate-950">{isEdit ? '编辑文章' : '写文章'}</h1>
              <p className="mt-2 max-w-2xl text-sm leading-7 text-slate-500">标题、正文、分类和封面都会走 Java/Kotlin 后端同一套 API 契约。</p>
            </div>
          </Card.Header>
          <Card.Content>
            <form onSubmit={onSubmit} className="space-y-5">
              <Field label="标题">
                <Input
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  required
                  maxLength={200}
                  placeholder="例如：Redis 缓存文章详情链路"
                />
              </Field>
              <Field label="分类(可选)">
                <Input value={category} onChange={(e) => setCategory(e.target.value)} maxLength={50} placeholder="Java / Kotlin / Redis" />
              </Field>
              <Field label="正文" hint="富文本会以 HTML 保存到文章 content 字段，详情页会清洗后渲染。">
                <RichTextEditor
                  value={content}
                  onChange={setContent}
                  placeholder="记录这条业务链路的入口、服务、缓存、数据库和异常处理..."
                />
              </Field>
              <div className="space-y-2">
                <span className="block text-sm font-semibold text-slate-700">封面图(可选)</span>
                <label className="grid min-h-36 cursor-pointer place-items-center gap-3 rounded-2xl border border-dashed border-teal-300 bg-teal-50/70 p-4 text-teal-800 transition hover:border-teal-500 hover:bg-teal-50">
                  <ImagePlus size={24} />
                  <span className="text-sm font-semibold">{uploading ? '上传中…' : '选择一张图片上传到 MinIO'}</span>
                  <input type="file" accept="image/*" onChange={onPickCover} className="sr-only" />
                </label>
                {coverUrl && (
                  <img
                    src={coverUrl}
                    alt="封面预览"
                    className="max-h-56 w-full rounded-2xl border border-slate-200 object-cover"
                  />
                )}
              </div>
              <ErrorText>{error}</ErrorText>
              <div className="flex gap-3">
                <Button type="submit" disabled={saving || uploading}>
                  <Save size={17} />
                  {saving ? '保存中…' : '保存'}
                </Button>
                <GhostButton type="button" onClick={() => navigate(-1)}>
                  <X size={17} />
                  取消
                </GhostButton>
              </div>
            </form>
          </Card.Content>
        </Card>
        </div>
      </div>
    </AppShell>
  )
}
