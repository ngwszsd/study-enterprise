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

/** 带 reason 的鉴权错误会原样传给前端,便于区分 token 过期、文档不匹配、只读等情况。 */
export class CollabAuthError extends Error {
  constructor(
    message: string,
    readonly reason: string,
  ) {
    super(message)
  }
}

/**
 * 轻量 JWT 校验器。
 *
 * 这里没有引入完整 Passport/JWT 体系,是为了让 Nest 协作服务保持薄:只校验 Spring 后端签发的短期
 * typ=collab token,不参与普通登录态。
 */
// @Injectable(): 让 JwtVerifier 成为可注入服务,供 CollabServer 在握手鉴权时使用。
@Injectable()
export class JwtVerifier {
  // @Inject(ConfigService): 从 Nest 容器取配置服务,而不是在这里直接读散落的 process.env。
  constructor(@Inject(ConfigService) private readonly config: ConfigService) {}

  /** 校验格式、算法、签名、typ 和过期时间;业务成员权限在 Spring 后端签发 token 前已经校验。 */
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

/** 根据 JWT header alg 选择对应 HMAC,和 jjwt 生成的 HS256/384/512 签名保持一致。 */
function sign(input: string, secret: string, alg: string): string {
  const algorithm = alg === 'HS512' ? 'sha512' : alg === 'HS384' ? 'sha384' : 'sha256'
  return createHmac(algorithm, secret).update(input).digest('base64url')
}

function decodeJson<T>(part: string): T {
  return JSON.parse(Buffer.from(part, 'base64url').toString('utf8')) as T
}

/** timingSafeEqual 避免签名比较出现明显时序差异;长度不同先返回 false。 */
function safeEqual(a: string, b: string): boolean {
  const left = Buffer.from(a)
  const right = Buffer.from(b)
  return left.length === right.length && timingSafeEqual(left, right)
}
