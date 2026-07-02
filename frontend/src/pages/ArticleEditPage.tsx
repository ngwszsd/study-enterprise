import { useEffect, useState, type ChangeEvent, type FormEvent } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { createArticle, getArticle, updateArticle } from '../api/articles'
import { errorMessage } from '../api/client'
import { uploadFile } from '../api/files'
import { Button, ErrorText, Field, GhostButton, Input, Spinner, Textarea } from '../components/ui'

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
    <div className="mx-auto max-w-2xl px-4 py-6">
      <h1 className="mb-6 text-2xl font-bold text-gray-900">{isEdit ? '编辑文章' : '写文章'}</h1>
      <form onSubmit={onSubmit} className="space-y-4">
        <Field label="标题">
          <Input value={title} onChange={(e) => setTitle(e.target.value)} required maxLength={200} />
        </Field>
        <Field label="分类(可选)">
          <Input value={category} onChange={(e) => setCategory(e.target.value)} maxLength={50} />
        </Field>
        <Field label="正文">
          <Textarea value={content} onChange={(e) => setContent(e.target.value)} required rows={10} />
        </Field>
        <div className="space-y-2">
          <span className="text-sm font-medium text-gray-700">封面图(可选)</span>
          <input type="file" accept="image/*" onChange={onPickCover} className="block text-sm" />
          {uploading && <p className="text-sm text-gray-400">上传中…</p>}
          {coverUrl && <img src={coverUrl} alt="封面预览" className="max-h-48 rounded-lg object-cover" />}
        </div>
        <ErrorText>{error}</ErrorText>
        <div className="flex gap-3">
          <Button type="submit" disabled={saving || uploading}>
            {saving ? '保存中…' : '保存'}
          </Button>
          <GhostButton type="button" onClick={() => navigate(-1)}>
            取消
          </GhostButton>
        </div>
      </form>
    </div>
  )
}
