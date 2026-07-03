import 'reflect-metadata'
import { NestFactory } from '@nestjs/core'
import { AppModule } from './app.module.js'

async function bootstrap() {
  const app = await NestFactory.createApplicationContext(AppModule, {
    logger: ['log', 'warn', 'error'],
  })

  const close = async () => {
    await app.close()
    process.exit(0)
  }

  process.on('SIGINT', close)
  process.on('SIGTERM', close)
}

bootstrap()
