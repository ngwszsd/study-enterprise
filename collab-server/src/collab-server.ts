import { Injectable, Logger, OnApplicationBootstrap, OnApplicationShutdown } from '@nestjs/common'
import { Server } from '@hocuspocus/server'
import * as Y from 'yjs'
import { BackendClient } from './backend-client.js'
import { ConfigService } from './config.service.js'
import { JwtVerifier } from './jwt-verifier.js'

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
    private readonly backend: BackendClient,
    private readonly config: ConfigService,
    private readonly jwtVerifier: JwtVerifier,
  ) {}

  async onApplicationBootstrap() {
    this.server = new Server<CollabContext>({
      name: 'study-collab-server',
      port: this.config.port,
      stopOnSignals: false,
      debounce: 1000,
      maxDebounce: 5000,

      onAuthenticate: async ({ token, documentName, context, connectionConfig }) => {
        const claims = this.jwtVerifier.verify(token)
        if (claims.docName !== documentName) {
          throw new Error('协作 token 与文档不匹配')
        }
        if (claims.role === 'VIEWER') {
          throw new Error('只读成员暂不能进入协作编辑')
        }

        context.userId = Number(claims.sub)
        context.username = claims.username
        context.noteId = claims.noteId
        context.role = claims.role
        connectionConfig.readOnly = false
      },

      onLoadDocument: async ({ documentName, document }) => {
        const noteId = noteIdFromDocumentName(documentName)
        const state = await this.backend.loadDocument(noteId)
        if (state.length > 0) {
          Y.applyUpdate(document, state)
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
