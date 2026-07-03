import { Injectable } from '@nestjs/common'
import { ConfigService } from './config.service.js'

@Injectable()
export class BackendClient {
  constructor(private readonly config: ConfigService) {}

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
