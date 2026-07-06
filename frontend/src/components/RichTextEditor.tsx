import Collaboration from '@tiptap/extension-collaboration'
import CollaborationCaret from '@tiptap/extension-collaboration-caret'
import Highlight from '@tiptap/extension-highlight'
import LinkExtension from '@tiptap/extension-link'
import Placeholder from '@tiptap/extension-placeholder'
import TaskItem from '@tiptap/extension-task-item'
import TaskList from '@tiptap/extension-task-list'
import TextAlign from '@tiptap/extension-text-align'
import UnderlineExtension from '@tiptap/extension-underline'
import { EditorContent, useEditor, type Editor, type UseEditorOptions } from '@tiptap/react'
import StarterKit from '@tiptap/starter-kit'
import { Card } from '@heroui/react'
import {
  AlignCenter,
  AlignLeft,
  AlignRight,
  Bold,
  Code2,
  Eraser,
  Heading1,
  Heading2,
  Heading3,
  Highlighter,
  Italic,
  Link as LinkIcon,
  List,
  ListOrdered,
  ListTodo,
  Minus,
  Pilcrow,
  Quote,
  Redo2,
  Strikethrough,
  Undo2,
  Unlink,
  Underline,
} from 'lucide-react'
import { useEffect, useMemo, type ReactNode } from 'react'
import type { HocuspocusProvider } from '@hocuspocus/provider'
import type { Doc as YDoc } from 'yjs'

export type RichTextEditorHandle = Editor

interface CollaborationUser {
  id: number
  name: string
  color: string
}

interface RichTextEditorProps {
  value?: string
  onChange?: (value: string) => void
  placeholder?: string
  editable?: boolean
  collaboration?: {
    document: YDoc
    field?: string
    provider?: HocuspocusProvider | null
    user?: CollaborationUser | null
  }
  minHeightClassName?: string
  onEditorReady?: (editor: Editor) => void
}

export function RichTextEditor({
  value = '',
  onChange,
  placeholder = '输入正文内容…',
  editable = true,
  collaboration,
  minHeightClassName = 'min-h-[360px]',
  onEditorReady,
}: RichTextEditorProps) {
  const isCollaborative = Boolean(collaboration?.document)
  const extensions = useMemo(() => {
    const baseExtensions: NonNullable<UseEditorOptions['extensions']> = [
      StarterKit.configure({
        undoRedo: isCollaborative ? false : undefined,
        link: false,
        underline: false,
      }),
      LinkExtension.configure({
        openOnClick: false,
        autolink: true,
        defaultProtocol: 'https',
        HTMLAttributes: {
          class: 'text-teal-700 underline underline-offset-4',
        },
      }),
      UnderlineExtension,
      Highlight.configure({
        multicolor: false,
        HTMLAttributes: {
          class: 'rounded bg-amber-100 px-0.5',
        },
      }),
      TextAlign.configure({
        types: ['heading', 'paragraph'],
      }),
      TaskList.configure({
        HTMLAttributes: {
          class: 'not-prose',
        },
      }),
      TaskItem.configure({
        nested: true,
      }),
      Placeholder.configure({
        placeholder,
      }),
    ]

    if (collaboration?.document) {
      baseExtensions.push(
        Collaboration.configure({
          document: collaboration.document,
          field: collaboration.field ?? 'default',
        }),
      )
    }

    if (collaboration?.provider && collaboration.user) {
      baseExtensions.push(
        CollaborationCaret.configure({
          provider: collaboration.provider,
          user: collaboration.user,
          render: (user) => renderCollaborationCaret(user),
          selectionRender: (user) => ({
            nodeName: 'span',
            class: 'collaboration-caret-selection',
            style: `background-color: ${transparentColor(user.color, '26')}; border-radius: 2px;`,
            'data-user': user.name,
          }),
        }),
      )
    }

    return baseExtensions
  }, [
    collaboration?.document,
    collaboration?.field,
    collaboration?.provider,
    collaboration?.user?.color,
    collaboration?.user?.id,
    collaboration?.user?.name,
    isCollaborative,
    placeholder,
  ])

  const editor = useEditor({
    extensions,
    content: isCollaborative ? undefined : value || '<p></p>',
    editable,
    autofocus: false,
    immediatelyRender: true,
    shouldRerenderOnTransaction: true,
    editorProps: {
      attributes: {
        class: `${minHeightClassName} cursor-text rounded-b-2xl bg-white px-4 py-4 text-sm leading-7 text-slate-800 outline-none [&_.is-editor-empty:first-child::before]:pointer-events-none [&_.is-editor-empty:first-child::before]:float-left [&_.is-editor-empty:first-child::before]:h-0 [&_.is-editor-empty:first-child::before]:text-slate-400 [&_.is-editor-empty:first-child::before]:content-[attr(data-placeholder)] [&_blockquote]:rounded-xl [&_blockquote]:border-l-4 [&_blockquote]:border-teal-200 [&_blockquote]:bg-teal-50 [&_blockquote]:px-4 [&_blockquote]:py-2 [&_code]:rounded [&_code]:bg-slate-100 [&_code]:px-1.5 [&_code]:py-0.5 [&_code]:text-[0.85em] [&_h1]:text-2xl [&_h1]:font-bold [&_h2]:text-xl [&_h2]:font-bold [&_h3]:text-lg [&_h3]:font-bold [&_hr]:my-5 [&_hr]:border-slate-200 [&_mark]:rounded [&_mark]:bg-amber-100 [&_ol]:list-decimal [&_ol]:pl-6 [&_p]:my-2 [&_pre]:overflow-auto [&_pre]:rounded-xl [&_pre]:bg-slate-950 [&_pre]:p-4 [&_pre_code]:bg-transparent [&_pre_code]:p-0 [&_pre_code]:text-slate-100 [&_ul:not([data-type='taskList'])]:list-disc [&_ul]:pl-6 [&_ul[data-type='taskList']_input]:mt-1 [&_ul[data-type='taskList']_input]:accent-teal-600 [&_ul[data-type='taskList']_li>div]:min-w-0 [&_ul[data-type='taskList']_li>label]:mt-1 [&_ul[data-type='taskList']_li]:flex [&_ul[data-type='taskList']_li]:gap-2 [&_ul[data-type='taskList']]:list-none [&_ul[data-type='taskList']]:pl-0`,
      },
    },
    onUpdate: ({ editor }) => {
      if (!isCollaborative) {
        onChange?.(editor.getHTML())
      }
    },
  }, [extensions, isCollaborative, minHeightClassName])

  useEffect(() => {
    if (!editor || isCollaborative) return
    if (value !== editor.getHTML()) {
      editor.commands.setContent(value || '<p></p>', { emitUpdate: false })
    }
  }, [editor, isCollaborative, value])

  useEffect(() => {
    if (!editor) return
    editor.setEditable(editable)
  }, [editable, editor])

  useEffect(() => {
    if (!editor) return
    onEditorReady?.(editor)
  }, [editor, onEditorReady])

  if (!editor) return null

  const setLink = () => {
    if (!editable) return
    const previousUrl = editor.getAttributes('link').href as string | undefined
    const url = window.prompt('输入链接地址', previousUrl ?? 'https://')
    if (url === null) return
    if (url.trim() === '') {
      editor.chain().focus().unsetLink().run()
      return
    }
    editor.chain().focus().extendMarkRange('link').setLink({ href: url }).run()
  }

  return (
    <Card className="overflow-hidden border border-slate-200 bg-white shadow-sm">
      <Card.Header className="flex-row flex-wrap gap-2 border-b border-slate-200 bg-slate-50/80 p-3">
        <ToolbarButton active={editor.isActive('paragraph')} label="段落" onPress={() => editor.chain().focus().setParagraph().run()} isDisabled={!editable}>
          <Pilcrow size={16} />
        </ToolbarButton>
        <ToolbarButton
          active={editor.isActive('heading', { level: 1 })}
          label="一级标题"
          onPress={() => editor.chain().focus().toggleHeading({ level: 1 }).run()}
          isDisabled={!editable}
        >
          <Heading1 size={16} />
        </ToolbarButton>
        <ToolbarButton
          active={editor.isActive('heading', { level: 2 })}
          label="二级标题"
          onPress={() => editor.chain().focus().toggleHeading({ level: 2 }).run()}
          isDisabled={!editable}
        >
          <Heading2 size={16} />
        </ToolbarButton>
        <ToolbarButton
          active={editor.isActive('heading', { level: 3 })}
          label="三级标题"
          onPress={() => editor.chain().focus().toggleHeading({ level: 3 }).run()}
          isDisabled={!editable}
        >
          <Heading3 size={16} />
        </ToolbarButton>

        <ToolbarDivider />

        <ToolbarButton active={editor.isActive('bold')} label="加粗" onPress={() => editor.chain().focus().toggleBold().run()} isDisabled={!editable || !editor.can().chain().focus().toggleBold().run()}>
          <Bold size={16} />
        </ToolbarButton>
        <ToolbarButton active={editor.isActive('italic')} label="斜体" onPress={() => editor.chain().focus().toggleItalic().run()} isDisabled={!editable || !editor.can().chain().focus().toggleItalic().run()}>
          <Italic size={16} />
        </ToolbarButton>
        <ToolbarButton active={editor.isActive('underline')} label="下划线" onPress={() => editor.chain().focus().toggleUnderline().run()} isDisabled={!editable}>
          <Underline size={16} />
        </ToolbarButton>
        <ToolbarButton active={editor.isActive('strike')} label="删除线" onPress={() => editor.chain().focus().toggleStrike().run()} isDisabled={!editable}>
          <Strikethrough size={16} />
        </ToolbarButton>
        <ToolbarButton active={editor.isActive('code')} label="行内代码" onPress={() => editor.chain().focus().toggleCode().run()} isDisabled={!editable || !editor.can().chain().focus().toggleCode().run()}>
          <Code2 size={16} />
        </ToolbarButton>
        <ToolbarButton active={editor.isActive('highlight')} label="高亮" onPress={() => editor.chain().focus().toggleHighlight().run()} isDisabled={!editable}>
          <Highlighter size={16} />
        </ToolbarButton>

        <ToolbarDivider />

        <ToolbarButton active={editor.isActive('bulletList')} label="无序列表" onPress={() => editor.chain().focus().toggleBulletList().run()} isDisabled={!editable}>
          <List size={16} />
        </ToolbarButton>
        <ToolbarButton active={editor.isActive('orderedList')} label="有序列表" onPress={() => editor.chain().focus().toggleOrderedList().run()} isDisabled={!editable}>
          <ListOrdered size={16} />
        </ToolbarButton>
        <ToolbarButton active={editor.isActive('taskList')} label="任务列表" onPress={() => editor.chain().focus().toggleTaskList().run()} isDisabled={!editable}>
          <ListTodo size={16} />
        </ToolbarButton>
        <ToolbarButton active={editor.isActive('blockquote')} label="引用" onPress={() => editor.chain().focus().toggleBlockquote().run()} isDisabled={!editable}>
          <Quote size={16} />
        </ToolbarButton>
        <ToolbarButton active={editor.isActive('codeBlock')} label="代码块" onPress={() => editor.chain().focus().toggleCodeBlock().run()} isDisabled={!editable}>
          <Code2 size={16} />
        </ToolbarButton>
        <ToolbarButton label="分割线" onPress={() => editor.chain().focus().setHorizontalRule().run()} isDisabled={!editable}>
          <Minus size={16} />
        </ToolbarButton>

        <ToolbarDivider />

        <ToolbarButton active={editor.isActive({ textAlign: 'left' })} label="左对齐" onPress={() => editor.chain().focus().setTextAlign('left').run()} isDisabled={!editable}>
          <AlignLeft size={16} />
        </ToolbarButton>
        <ToolbarButton active={editor.isActive({ textAlign: 'center' })} label="居中" onPress={() => editor.chain().focus().setTextAlign('center').run()} isDisabled={!editable}>
          <AlignCenter size={16} />
        </ToolbarButton>
        <ToolbarButton active={editor.isActive({ textAlign: 'right' })} label="右对齐" onPress={() => editor.chain().focus().setTextAlign('right').run()} isDisabled={!editable}>
          <AlignRight size={16} />
        </ToolbarButton>

        <ToolbarDivider />

        <ToolbarButton active={editor.isActive('link')} label="设置链接" onPress={setLink} isDisabled={!editable}>
          <LinkIcon size={16} />
        </ToolbarButton>
        <ToolbarButton label="取消链接" onPress={() => editor.chain().focus().unsetLink().run()} isDisabled={!editable || !editor.isActive('link')}>
          <Unlink size={16} />
        </ToolbarButton>
        <ToolbarButton label="清除格式" onPress={() => editor.chain().focus().unsetAllMarks().clearNodes().run()} isDisabled={!editable}>
          <Eraser size={16} />
        </ToolbarButton>

        <ToolbarDivider />

        <ToolbarButton label="撤销" onPress={() => editor.chain().focus().undo().run()} isDisabled={!editable || !editor.can().undo()}>
          <Undo2 size={16} />
        </ToolbarButton>
        <ToolbarButton label="重做" onPress={() => editor.chain().focus().redo().run()} isDisabled={!editable || !editor.can().redo()}>
          <Redo2 size={16} />
        </ToolbarButton>
      </Card.Header>
      <Card.Content className="p-0">
        <EditorContent editor={editor} onMouseDown={() => editor.commands.focus()} />
      </Card.Content>
    </Card>
  )
}

function renderCollaborationCaret(user: Record<string, any>) {
  const color = typeof user.color === 'string' ? user.color : '#0f766e'
  const name = typeof user.name === 'string' && user.name.trim() ? user.name : '协作者'
  const cursor = document.createElement('span')
  cursor.classList.add('collaboration-caret')
  cursor.style.borderLeft = `2px solid ${color}`
  cursor.style.borderRight = `2px solid ${color}`
  cursor.style.marginLeft = '-1px'
  cursor.style.marginRight = '-1px'
  cursor.style.pointerEvents = 'none'
  cursor.style.position = 'relative'
  cursor.style.wordBreak = 'normal'

  const label = document.createElement('span')
  label.classList.add('collaboration-caret-label')
  label.textContent = name
  label.style.position = 'absolute'
  label.style.left = '-1px'
  label.style.top = '-1.55rem'
  label.style.zIndex = '30'
  label.style.maxWidth = '180px'
  label.style.overflow = 'hidden'
  label.style.textOverflow = 'ellipsis'
  label.style.whiteSpace = 'nowrap'
  label.style.borderRadius = '6px'
  label.style.backgroundColor = color
  label.style.padding = '2px 8px'
  label.style.color = '#ffffff'
  label.style.fontSize = '12px'
  label.style.fontWeight = '700'
  label.style.lineHeight = '18px'
  label.style.boxShadow = '0 8px 20px rgba(15, 23, 42, 0.16)'

  cursor.appendChild(label)
  return cursor
}

function transparentColor(color: unknown, alphaHex: string) {
  if (typeof color === 'string' && /^#[0-9a-fA-F]{6}$/.test(color)) {
    return `${color}${alphaHex}`
  }
  return 'rgba(20, 184, 166, 0.18)'
}

function ToolbarDivider() {
  return <span aria-hidden="true" className="my-1 h-6 w-px shrink-0 bg-slate-200" />
}

function ToolbarButton({
  active,
  label,
  children,
  isDisabled,
  onPress,
}: {
  active?: boolean
  label: string
  children: ReactNode
  isDisabled?: boolean
  onPress: () => void
}) {
  return (
    <button
      type="button"
      tabIndex={-1}
      aria-label={label}
      disabled={isDisabled}
      onMouseDown={(event) => {
        event.preventDefault()
        if (!isDisabled) {
          onPress()
        }
      }}
      onClick={(event) => {
        event.preventDefault()
      }}
      className={[
        'inline-grid h-8 w-8 shrink-0 place-items-center rounded-lg border text-sm transition',
        'focus:outline-none focus-visible:outline-none',
        isDisabled
          ? 'cursor-not-allowed border-slate-200 bg-slate-100 text-slate-300'
          : active
            ? 'border-teal-200 bg-teal-50 text-teal-700'
            : 'border-transparent text-slate-600 hover:border-slate-200 hover:bg-white hover:text-slate-950',
      ].join(' ')}
    >
      {children}
    </button>
  )
}
