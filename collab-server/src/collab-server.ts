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

@Injectable()
export class CollabServer implements OnApplicationBootstrap, OnApplicationShutdown {
  private readonly logger = new Logger(CollabServer.name)
  private server?: Server<CollabContext>

  constructor(
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

      onAuthenticate: async ({ token, documentName, context, connectionConfig }) => {
        try {
          const claims = this.jwtVerifier.verify(token)
          if (claims.docName !== documentName) {
            throw new CollabAuthError('协作 token 与文档不匹配', 'document-mismatch')
          }
          if (claims.role === 'VIEWER') {
            throw new CollabAuthError('只读成员暂不能进入协作编辑', 'viewer-readonly')
          }

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

      onStoreDocument: async ({ documentName, document }) => {
        const noteId = noteIdFromDocumentName(documentName)
        const state = Y.encodeStateAsUpdate(document)
        await this.backend.saveDocument(noteId, state)
      },
    })

    await this.server.listen()
    this.logger.log(`Hocuspocus 协作服务已启动: ws://localhost:${this.config.port}`)
    this.logger.log(`文档快照后端: ${this.config.backendUrl}`)
  }

  async onApplicationShutdown() {
    if (this.server) {
      this.server.hocuspocus.flushPendingStores()
      await this.server.destroy()
    }
  }
}

function noteIdFromDocumentName(documentName: string): number {
  const match = /^note:(\d+)$/.exec(documentName)
  if (!match) {
    throw new Error('文档名必须是 note:{id}')
  }
  return Number(match[1])
}
