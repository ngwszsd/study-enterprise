import {
  Button as HeroButton,
  Input as HeroInput,
  Spinner as HeroSpinner,
  TextArea as HeroTextarea,
} from '@heroui/react'
import type { ComponentProps, ReactNode } from 'react'

type HeroButtonProps = Omit<ComponentProps<typeof HeroButton>, 'className'> & {
  className?: string
  disabled?: boolean
}
type HeroInputProps = ComponentProps<typeof HeroInput>
type HeroTextareaProps = ComponentProps<typeof HeroTextarea>

function cx(...classes: Array<string | false | null | undefined>) {
  return classes.filter(Boolean).join(' ')
}

export function Button({ className = '', disabled, isDisabled, ...props }: HeroButtonProps) {
  return (
    <HeroButton
      variant="primary"
      className={cx('min-w-24', className)}
      isDisabled={isDisabled ?? disabled}
      {...props}
    />
  )
}

export function GhostButton({ className = '', disabled, isDisabled, ...props }: HeroButtonProps) {
  return (
    <HeroButton
      variant="outline"
      className={cx('min-w-20', className)}
      isDisabled={isDisabled ?? disabled}
      {...props}
    />
  )
}

export function DangerButton({ className = '', disabled, isDisabled, ...props }: HeroButtonProps) {
  return (
    <HeroButton
      variant="danger-soft"
      className={cx('min-w-20', className)}
      isDisabled={isDisabled ?? disabled}
      {...props}
    />
  )
}

export function Input({ className = '', fullWidth = true, ...props }: HeroInputProps) {
  return <HeroInput variant="primary" fullWidth={fullWidth} className={className} {...props} />
}

export function Textarea({ className = '', fullWidth = true, ...props }: HeroTextareaProps) {
  return <HeroTextarea variant="primary" fullWidth={fullWidth} className={className} {...props} />
}

export function Field({
  label,
  hint,
  children,
}: {
  label: string
  hint?: string
  children: ReactNode
}) {
  return (
    <div className="block space-y-2">
      <span className="block text-sm font-semibold text-slate-700">{label}</span>
      {children}
      {hint && <span className="block text-xs leading-5 text-slate-500">{hint}</span>}
    </div>
  )
}

export function ErrorText({ children }: { children?: ReactNode }) {
  if (!children) return null
  return (
    <p className="rounded-xl border border-rose-200 bg-rose-50 px-3 py-2 text-sm leading-6 text-rose-700">
      {children}
    </p>
  )
}

export function Spinner({ text = '加载中…' }: { text?: string }) {
  return (
    <div className="flex min-h-52 items-center justify-center gap-3 p-8 text-slate-500">
      <HeroSpinner size="sm" color="accent" />
      <span className="text-sm font-medium">{text}</span>
    </div>
  )
}
