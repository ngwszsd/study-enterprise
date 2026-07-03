package com.study.sse

import com.study.event.ArticleCreatedEvent
import com.study.exception.UnauthorizedException
import com.study.security.JwtService
import java.util.concurrent.ConcurrentHashMap
import org.springframework.context.event.EventListener
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

/** SSE 通知流:文章创建时向所有连接单向推送(浏览器用 EventSource,token 走查询参数)。 */
@RestController
@RequestMapping("/api/sse")
class NotificationController(private val jwtService: JwtService) {

    private val emitters = ConcurrentHashMap.newKeySet<SseEmitter>()

    @GetMapping("/notifications")
    fun notifications(@RequestParam token: String): SseEmitter {
        try {
            jwtService.parse(token)
        } catch (e: Exception) {
            throw UnauthorizedException("无效令牌")
        }
        val emitter = SseEmitter(0L) // 0 = 不超时
        emitters.add(emitter)
        emitter.onCompletion { emitters.remove(emitter) }
        emitter.onTimeout { emitters.remove(emitter) }
        try {
            emitter.send(SseEmitter.event().name("connected").data("SSE 已连接"))
        } catch (e: Exception) {
            emitters.remove(emitter)
        }
        return emitter
    }

    @EventListener
    fun onArticleCreated(event: ArticleCreatedEvent) {
        emitters.forEach { emitter ->
            try {
                emitter.send(
                    SseEmitter.event()
                        .name("article-created")
                        .data(mapOf("id" to event.id, "title" to event.title, "author" to event.author)),
                )
            } catch (e: Exception) {
                emitters.remove(emitter)
            }
        }
    }
}
