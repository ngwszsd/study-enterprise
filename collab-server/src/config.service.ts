import { Injectable } from '@nestjs/common'

@Injectable()
export class ConfigService {
  readonly port = Number(process.env.COLLAB_PORT ?? 19082)
  readonly backendUrl = trimSlash(process.env.COLLAB_BACKEND_URL ?? 'http://localhost:18080')
  readonly internalSecret = process.env.COLLAB_INTERNAL_SECRET ?? 'dev-collab-secret'
  readonly jwtSecret = process.env.JWT_SECRET ?? 'dev-only-change-me-please-a-long-random-secret-32bytes-min'
}

function trimSlash(value: string): string {
  return value.endsWith('/') ? value.slice(0, -1) : value
}
