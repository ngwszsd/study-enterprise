import 'reflect-metadata'
import { NestFactory } from '@nestjs/core'
import { AppModule } from './app.module.js'

// 这个 Nest 应用不暴露传统 REST Controller,只作为 DI 容器启动 Hocuspocus WebSocket 服务。
// 真正的监听端口在 CollabServer.onApplicationBootstrap() 里创建。
async function bootstrap() {
  const app = await NestFactory.createApplicationContext(AppModule, {
    logger: ['log', 'warn', 'error'],
  })

  // 用 Nest 生命周期优雅关闭 Hocuspocus,确保退出前 flush 未落库的 Yjs 快照。
  const close = async () => {
    await app.close()
    process.exit(0)
  }

  process.on('SIGINT', close)
  process.on('SIGTERM', close)
}

bootstrap()
