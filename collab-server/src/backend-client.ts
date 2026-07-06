import { Inject, Injectable } from '@nestjs/common'
import { ConfigService } from './config.service.js'

/**
 * Nest 协作服务访问 Java/Kotlin 后端的内部客户端。
 *
 * 注意职责边界:业务权限、成员关系、数据库表都归 Spring 后端管;Nest 只在 Hocuspocus 需要加载/保存
 * Yjs binary update 时调用 /api/notes/internal/{id}/document。
 */
// @Injectable(): 注册为 Nest provider;生命周期由 Nest 管,不是手动 new。
@Injectable()
export class BackendClient {
  // @Inject(ConfigService): 显式告诉 Nest 注入哪个 provider token。这里写清楚便于学习 DI 关系。
  constructor(@Inject(ConfigService) private readonly config: ConfigService) {}

  /** 从 Spring 后端读取 Base64 包装的 Yjs update,再转回 Uint8Array 给 Hocuspocus。 */
  async loadDocument(noteId: number): Promise<Uint8Array> {
    const res = await fetch(`${this.config.backendUrl}/api/notes/internal/${noteId}/document`, {
      headers: this.headers(),
    })
    if (!res.ok) {
      throw new Error(`加载协作文档失败: HTTP ${res.status}`)
    }
    const body = (await res.json()) as { state?: string }
    return fromBase64(body.state ?? '')
  }

  /** Hocuspocus debounce 后触发持久化,这里把二进制 update Base64 化后写回 Spring 后端。 */
  async saveDocument(noteId: number, state: Uint8Array): Promise<void> {
    const res = await fetch(`${this.config.backendUrl}/api/notes/internal/${noteId}/document`, {
      method: 'PUT',
      headers: {
        ...this.headers(),
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ state: toBase64(state) }),
    })
    if (!res.ok) {
      throw new Error(`保存协作文档失败: HTTP ${res.status}`)
    }
  }

  /** 内部接口不走用户 JWT,只认共享密钥。 */
  private headers(): Record<string, string> {
    return { 'X-Collab-Secret': this.config.internalSecret }
  }
}

function fromBase64(value: string): Uint8Array {
  if (!value) return new Uint8Array()
  return Uint8Array.from(Buffer.from(value, 'base64'))
}

function toBase64(value: Uint8Array): string {
  return Buffer.from(value).toString('base64')
}
