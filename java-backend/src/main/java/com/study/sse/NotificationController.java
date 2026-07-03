package com.study.sse;

import com.study.event.ArticleCreatedEvent;
import com.study.exception.UnauthorizedException;
import com.study.security.JwtService;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.event.EventListener;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** SSE 通知流:文章创建时向所有连接单向推送(浏览器用 EventSource,token 走查询参数)。 */
@RestController
@RequestMapping("/api/sse")
public class NotificationController {

    private final JwtService jwtService;
    private final Set<SseEmitter> emitters = ConcurrentHashMap.newKeySet();

    public NotificationController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @GetMapping("/notifications")
    public SseEmitter notifications(@RequestParam String token) {
        try {
            jwtService.parse(token);
        } catch (Exception e) {
            throw new UnauthorizedException("无效令牌");
        }
        SseEmitter emitter = new SseEmitter(0L); // 0 = 不超时
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        try {
            emitter.send(SseEmitter.event().name("connected").data("SSE 已连接"));
        } catch (IOException e) {
            emitters.remove(emitter);
        }
        return emitter;
    }

    @EventListener
    public void onArticleCreated(ArticleCreatedEvent event) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("article-created")
                        .data(Map.of("id", event.id(), "title", event.title(), "author", event.author())));
            } catch (IOException e) {
                emitters.remove(emitter);
            }
        }
    }
}
