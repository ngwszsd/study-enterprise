import { Button, Chip } from '@heroui/react'
import { FileText, LogOut, MessageSquareText, Network, NotebookTabs } from 'lucide-react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

const navItems = [
  { to: '/articles', label: '文章', icon: FileText },
  { to: '/notes', label: '协作笔记', icon: NotebookTabs },
  { to: '/realtime', label: '实时', icon: MessageSquareText },
]

export function AppShell({ children }: { children: React.ReactNode }) {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  const onLogout = () => {
    logout()
    navigate('/login')
  }

  return (
    <div className="min-h-screen">
      <header className="sticky top-0 z-40 border-b border-slate-200/80 bg-white/85 backdrop-blur-xl">
        <div className="mx-auto flex max-w-7xl items-center justify-between gap-4 px-4 py-3">
          <Link to="/articles" className="flex items-center gap-3">
            <div className="grid h-10 w-10 place-items-center rounded-xl bg-slate-950 text-white shadow-sm">
              <Network size={18} strokeWidth={2.4} />
            </div>
            <div>
              <div className="font-bold text-slate-950">study-enterprise</div>
              <div className="text-xs font-medium text-slate-500">Java · Kotlin · React · Yjs</div>
            </div>
          </Link>

          <nav className="hidden rounded-2xl border border-slate-200 bg-white p-1 shadow-sm md:flex">
            {navItems.map((item) => {
              const active = location.pathname.startsWith(item.to)
              const Icon = item.icon
              return (
                <Button
                  key={item.to}
                  type="button"
                  size="sm"
                  variant={active ? 'secondary' : 'ghost'}
                  className={active ? 'text-teal-700' : 'text-slate-600'}
                  onPress={() => navigate(item.to)}
                >
                  <Icon size={15} />
                  {item.label}
                </Button>
              )
            })}
          </nav>

          <div className="flex items-center gap-2">
            {user && (
              <Chip size="sm" variant="soft" color="success">
                {user.username}
              </Chip>
            )}
            <Button size="sm" variant="outline" onPress={onLogout}>
              <LogOut size={15} />
              <span className="hidden sm:inline">退出</span>
            </Button>
          </div>
        </div>

        <nav className="mx-auto flex max-w-7xl gap-2 overflow-x-auto px-4 pb-3 md:hidden">
          {navItems.map((item) => {
            const active = location.pathname.startsWith(item.to)
            const Icon = item.icon
            return (
              <Button
                key={item.to}
                type="button"
                size="sm"
                variant={active ? 'secondary' : 'ghost'}
                className="shrink-0"
                onPress={() => navigate(item.to)}
              >
                <Icon size={15} />
                {item.label}
              </Button>
            )
          })}
        </nav>
      </header>
      <main>{children}</main>
    </div>
  )
}
