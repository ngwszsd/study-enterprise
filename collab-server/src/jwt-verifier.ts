import { createHmac, timingSafeEqual } from 'node:crypto'
import { Inject, Injectable } from '@nestjs/common'
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

export class CollabAuthError extends Error {
  constructor(
    message: string,
    readonly reason: string,
  ) {
    super(message)
  }
}

@Injectable()
export class JwtVerifier {
  constructor(@Inject(ConfigService) private readonly config: ConfigService) {}

  verify(token: string): CollabClaims {
    const [headerPart, payloadPart, signaturePart] = token.split('.')
    if (!headerPart || !payloadPart || !signaturePart) {
      throw new CollabAuthError('协作 token 格式无效', 'token-format-invalid')
    }

    const header = decodeJson<{ alg?: string }>(headerPart)
    if (!header.alg || !['HS256', 'HS384', 'HS512'].includes(header.alg)) {
      throw new CollabAuthError('协作 token 算法无效', 'token-alg-invalid')
    }

    const expected = sign(`${headerPart}.${payloadPart}`, this.config.jwtSecret, header.alg)
    if (!safeEqual(signaturePart, expected)) {
      throw new CollabAuthError('协作 token 签名无效', 'token-signature-invalid')
    }

    const claims = decodeJson<CollabClaims>(payloadPart)
    if (claims.typ !== 'collab') {
      throw new CollabAuthError('不是协作 token', 'token-type-invalid')
    }
    if (!claims.exp || claims.exp <= Math.floor(Date.now() / 1000)) {
      throw new CollabAuthError('协作 token 已过期', 'token-expired')
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
