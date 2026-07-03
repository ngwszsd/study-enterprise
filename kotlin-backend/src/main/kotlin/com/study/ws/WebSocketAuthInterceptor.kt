package com.study.ws

import com.study.security.JwtService
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.HandshakeInterceptor

/** WS 握手鉴权:从 ?token= 取 JWT 校验(浏览器 WS 不能带 header),通过则把 username 放进会话属性。 */
@Component
class WebSocketAuthInterceptor(private val jwtService: JwtService) : HandshakeInterceptor {

    override fun beforeHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        attributes: MutableMap<String, Any>,
    ): Boolean {
        val token = tokenFrom(request.uri.query) ?: return false
        return try {
            attributes["username"] = jwtService.parse(token).get("username", String::class.java) ?: "匿名"
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun afterHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        exception: Exception?,
    ) {
        // 无需处理
    }

    private fun tokenFrom(query: String?): String? =
        query?.split("&")
            ?.firstOrNull { it.startsWith("token=") }
            ?.let { URLDecoder.decode(it.substring("token=".length), StandardCharsets.UTF_8) }
}
