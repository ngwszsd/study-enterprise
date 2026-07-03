import { createHmac, timingSafeEqual } from 'node:crypto'
import { Injectable } from '@nestjs/common'
import { ConfigService } from './config.service.js'

export interface CollabClaims {
  sub: string
  username: string
  typ: string
  noteId: number
  docName: string
  role: 'OWNER' | 'EDITOR' | 'VIEWER'
  exp: number
}

@Injectable()
export class JwtVerifier {
  constructor(private readonly config: ConfigService) {}

  verify(token: string): CollabClaims {
    const [headerPart, payloadPart, signaturePart] = token.split('.')
    if (!headerPart || !payloadPart || !signaturePart) {
      throw new Error('协作 token 格式无效')
    }

    const header = decodeJson<{ alg?: string }>(headerPart)
    if (!header.alg || !['HS256', 'HS384', 'HS512'].includes(header.alg)) {
      throw new Error('协作 token 算法无效')
    }

    const expected = sign(`${headerPart}.${payloadPart}`, this.config.jwtSecret, header.alg)
    if (!safeEqual(signaturePart, expected)) {
      throw new Error('协作 token 签名无效')
    }

    const claims = decodeJson<CollabClaims>(payloadPart)
    if (claims.typ !== 'collab') {
      throw new Error('不是协作 token')
    }
    if (!claims.exp || claims.exp <= Math.floor(Date.now() / 1000)) {
      throw new Error('协作 token 已过期')
    }
    return claims
  }
}

function sign(input: string, secret: string, alg: string): string {
  const algorithm = alg === 'HS512' ? 'sha512' : alg === 'HS384' ? 'sha384' : 'sha256'
  return createHmac(algorithm, secret).update(input).digest('base64url')
}

function decodeJson<T>(part: string): T {
  return JSON.parse(Buffer.from(part, 'base64url').toString('utf8')) as T
}

function safeEqual(a: string, b: string): boolean {
  const left = Buffer.from(a)
  const right = Buffer.from(b)
  return left.length === right.length && timingSafeEqual(left, right)
}
