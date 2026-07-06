package com.study.config

import com.study.ws.ChatWebSocketHandler
import com.study.ws.WebSocketAuthInterceptor
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry

/**
 * 注册 WebSocket 端点 /ws/chat,带握手鉴权,允许前端来源。
 *
 * @Configuration 声明配置类;@EnableWebSocket 开启 Spring 原生 WebSocket 支持。
 */
@Configuration
@EnableWebSocket
class WebSocketConfig(
    private val chatHandler: ChatWebSocketHandler,
    private val authInterceptor: WebSocketAuthInterceptor,
) : WebSocketConfigurer {

    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        // /ws/chat 是原生 Spring WebSocket 端点,不同于协作编辑使用的 Hocuspocus ws://localhost:19082/collab/notes。
        registry.addHandler(chatHandler, "/ws/chat")
            .addInterceptors(authInterceptor)
            .setAllowedOrigins("http://localhost:15173")
    }
}
