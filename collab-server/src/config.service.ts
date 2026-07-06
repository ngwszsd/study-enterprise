import { Injectable } from '@nestjs/common'

// @Injectable(): 把普通 class 交给 Nest DI 容器管理,其他 provider 可以通过构造器注入它。
@Injectable()
export class ConfigService {
  // 协作服务自己的 WebSocket 端口,前端 HocuspocusProvider 会连这里。
  readonly port = Number(process.env.COLLAB_PORT ?? 19082)

  // 协作编辑 WebSocket 路径。不要直接暴露在 /,本地排查时能一眼看出它是笔记协作入口。
  readonly wsPath = normalizePath(process.env.COLLAB_WS_PATH ?? '/collab/notes')

  // 普通业务后端地址。默认连 Java 后端;学习 Kotlin 链路时用 COLLAB_BACKEND_URL 切过去。
  readonly backendUrl = trimSlash(process.env.COLLAB_BACKEND_URL ?? 'http://localhost:18080')

  // Nest -> Java/Kotlin 内部接口的共享密钥,避免普通浏览器直接读写 Yjs 快照。
  readonly internalSecret = process.env.COLLAB_INTERNAL_SECRET ?? 'dev-collab-secret'

  // 必须和 Java/Kotlin 后端签发 JWT 使用同一密钥,否则 collab token 无法验签。
  readonly jwtSecret = process.env.JWT_SECRET ?? 'dev-only-change-me-please-a-long-random-secret-32bytes-min'
}

function trimSlash(value: string): string {
  return value.endsWith('/') ? value.slice(0, -1) : value
}

function normalizePath(value: string): string {
  const trimmed = value.trim()
  const withLeadingSlash = trimmed.startsWith('/') ? trimmed : `/${trimmed}`
  return withLeadingSlash.length > 1 && withLeadingSlash.endsWith('/')
    ? withLeadingSlash.slice(0, -1)
    : withLeadingSlash
}
