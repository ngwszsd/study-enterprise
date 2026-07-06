import { Module } from '@nestjs/common'
import { BackendClient } from './backend-client.js'
import { CollabServer } from './collab-server.js'
import { ConfigService } from './config.service.js'
import { JwtVerifier } from './jwt-verifier.js'

// 协作服务的依赖关系很简单:
// ConfigService 读环境变量,JwtVerifier 校验短期 collab token,
// BackendClient 调 Java/Kotlin 后端内部接口,CollabServer 负责 Hocuspocus 生命周期。
// @Module(): Nest 的模块声明。providers 相当于把这些类注册成可被 DI 容器创建/注入的服务。
@Module({
  providers: [BackendClient, CollabServer, ConfigService, JwtVerifier],
})
export class AppModule {}
