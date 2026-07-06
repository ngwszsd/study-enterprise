import { Inject, Injectable, Logger, OnApplicationBootstrap, OnApplicationShutdown } from '@nestjs/common'
import { Server } from '@hocuspocus/server'
import * as Y from 'yjs'
import { BackendClient } from './backend-client.js'
import { ConfigService } from './config.service.js'
import { CollabAuthError, JwtVerifier } from './jwt-verifier.js'

interface CollabContext {
  userId: number
  username: string
  noteId: number
  role: 'OWNER' | 'EDITOR' | 'VIEWER'
}

/**
 * Hocuspocus 实时协作服务。
 *
 * 一次连接的完整顺序:
 * 1. 前端先向 Java/Kotlin 后端换取 5 分钟 collab token;
 * 2. Hocuspocus onAuthenticate 校验 token 并把用户/笔记/角色写入 context;
 * 3. onLoadDocument 从 Spring 后端读取已保存的 Yjs 快照;
 * 4. 多人编辑期间 Hocuspocus 在内存里同步 Yjs update/awareness;
 * 5. onStoreDocument debounce 后把最新 Yjs update 存回 Spring 后端。
 */
// @Injectable(): CollabServer 本身也是 Nest provider,这样它能被 AppModule 创建并触发生命周期 hook。
@Injectable()
export class CollabServer implements OnApplicationBootstrap, OnApplicationShutdown {
  private readonly logger = new Logger(CollabServer.name)
  private server?: Server<CollabContext>

  constructor(
    // @Inject(...): 这里显式声明依赖,对照 Spring 的构造器注入;Nest 会从 AppModule.providers 中找实例。
    @Inject(BackendClient) private readonly backend: BackendClient,
    @Inject(ConfigService) private readonly config: ConfigService,
    @Inject(JwtVerifier) private readonly jwtVerifier: JwtVerifier,
  ) {}

  async onApplicationBootstrap() {
    this.server = new Server<CollabContext>({
      name: 'study-collab-server',
      port: this.config.port,
      stopOnSignals: false,
      debounce: 1000,
      maxDebounce: 5000,

      // Hocuspocus Server 本身不提供 path 配置,这里在 upgrade 阶段只放行明确的协作笔记路径。
      onUpgrade: async ({ request, socket }) => {
        if (pathnameOf(request.url) !== this.config.wsPath) {
          const body = `Hocuspocus WebSocket endpoint: ${this.config.wsPath}\n`
          socket.write([
            'HTTP/1.1 404 Not Found',
            'Connection: close',
            'Content-Type: text/plain; charset=utf-8',
            `Content-Length: ${Buffer.byteLength(body)}`,
            '',
            body,
          ].join('\r\n'))
          socket.destroy()
          throw undefined
        }
      },

      // 每次 WebSocket 握手都会走这里。Nest 只认短期 collab token,不直接认普通登录 JWT。
      onAuthenticate: async ({ token, documentName, context, connectionConfig }) => {
        try {
          const claims = this.jwtVerifier.verify(token)
          if (claims.docName !== documentName) {
            throw new CollabAuthError('协作 token 与文档不匹配', 'document-mismatch')
          }
          if (claims.role === 'VIEWER') {
            throw new CollabAuthError('只读成员暂不能进入协作编辑', 'viewer-readonly')
          }

          // context 可被后续 Hocuspocus hook 使用;当前 MVP 主要用于日志/扩展,权限已在 token 阶段固定。
          context.userId = Number(claims.sub)
          context.username = claims.username
          context.noteId = claims.noteId
          context.role = claims.role
          connectionConfig.readOnly = false
        } catch (error) {
          const reason = error instanceof CollabAuthError ? error.reason : 'permission-denied'
          const message = error instanceof Error ? error.message : String(error)
          this.logger.warn(`协作鉴权失败 document=${documentName} reason=${reason}: ${message}`)
          throw new CollabAuthError(message, reason)
        }
      },

      // 文档首次被连接时加载快照。老数据/损坏数据不能让服务崩,所以失败时重置为空文档。
      onLoadDocument: async ({ documentName, document }) => {
        const noteId = noteIdFromDocumentName(documentName)
        const state = await this.backend.loadDocument(noteId)
        if (state.length > 0) {
          try {
            Y.applyUpdate(document, state)
          } catch (error) {
            const message = error instanceof Error ? error.message : String(error)
            this.logger.warn(`协作文档快照损坏，已重置为空文档 noteId=${noteId}: ${message}`)
            await this.backend.saveDocument(noteId, new Uint8Array())
          }
        }
      },

      // Hocuspocus 把多次变更 debounce 后集中持久化,避免每个按键都打 Spring 后端。
      onStoreDocument: async ({ documentName, document }) => {
        const noteId = noteIdFromDocumentName(documentName)
        const state = Y.encodeStateAsUpdate(document)
        await this.backend.saveDocument(noteId, state)
      },
    })

    await this.server.listen()
    this.logger.log(`Hocuspocus 协作服务已启动: ws://localhost:${this.config.port}${this.config.wsPath}`)
    this.logger.log(`文档快照后端: ${this.config.backendUrl}`)
  }

  // Nest 关闭时先 flush pending store,否则刚输入的内容可能还在 debounce 队列里。
  async onApplicationShutdown() {
    if (this.server) {
      this.server.hocuspocus.flushPendingStores()
      await this.server.destroy()
    }
  }
}

// 文档名约定由 Spring 后端 collab-token 返回,前后端都用 note:{id} 保持一致。
function noteIdFromDocumentName(documentName: string): number {
  const match = /^note:(\d+)$/.exec(documentName)
  if (!match) {
    throw new Error('文档名必须是 note:{id}')
  }
  return Number(match[1])
}

function pathnameOf(requestUrl: string | undefined): string {
  return new URL(requestUrl ?? '/', 'ws://localhost').pathname
}
