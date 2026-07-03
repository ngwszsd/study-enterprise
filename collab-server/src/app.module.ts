import { Module } from '@nestjs/common'
import { BackendClient } from './backend-client.js'
import { CollabServer } from './collab-server.js'
import { ConfigService } from './config.service.js'
import { JwtVerifier } from './jwt-verifier.js'

@Module({
  providers: [BackendClient, CollabServer, ConfigService, JwtVerifier],
})
export class AppModule {}
