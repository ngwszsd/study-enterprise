package com.study.ws

import com.fasterxml.jackson.databind.ObjectMapper
import com.study.event.ArticleCreatedEvent
import java.time.LocalTime
import java.util.concurrent.ConcurrentHashMap
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler

/** WebSocket 聊天室:客户端互发消息广播;文章创建时推送系统消息。 */
@Component
class ChatWebSocketHandler(private val objectMapper: ObjectMapper) : TextWebSocketHandler() {

    private val log = LoggerFactory.getLogger(javaClass)
    private val sessions = ConcurrentHashMap.newKeySet<WebSocketSession>()

    override fun afterConnectionEstablished(session: WebSocketSession) {
        sessions.add(session)
        broadcast(ChatMessage("system", "${usernameOf(session)} 加入了聊天室", now()))
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        val text = message.payload
        if (text.isNotBlank()) {
            broadcast(ChatMessage(usernameOf(session), text.trim(), now()))
        }
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        sessions.remove(session)
    }

    /** 文章创建事件 → 广播系统消息(WS 与业务通过 Spring 事件联动)。 */
    @EventListener
    fun onArticleCreated(event: ArticleCreatedEvent) {
        broadcast(ChatMessage("system", "📝 新文章:《${event.title}》 by ${event.author}", now()))
    }

    /** 同步广播,避免并发写同一 WS 会话。 */
    @Synchronized
    private fun broadcast(message: ChatMessage) {
        val payload = TextMessage(objectMapper.writeValueAsString(message))
        sessions.forEach { session ->
            try {
                if (session.isOpen) session.sendMessage(payload)
            } catch (e: Exception) {
                log.warn("WS 发送失败,移除会话", e)
                sessions.remove(session)
            }
        }
    }

    private fun usernameOf(session: WebSocketSession): String =
        session.attributes["username"]?.toString() ?: "匿名"

    private fun now(): String = LocalTime.now().withNano(0).toString()
}
