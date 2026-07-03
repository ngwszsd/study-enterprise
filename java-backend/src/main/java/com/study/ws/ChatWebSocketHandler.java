package com.study.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.event.ArticleCreatedEvent;
import java.io.IOException;
import java.time.LocalTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/** WebSocket 聊天室:客户端互发消息广播;文章创建时推送系统消息。 */
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketHandler.class);

    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final ObjectMapper objectMapper;

    public ChatWebSocketHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        broadcast(new ChatMessage("system", usernameOf(session) + " 加入了聊天室", now()));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String text = message.getPayload();
        if (text != null && !text.isBlank()) {
            broadcast(new ChatMessage(usernameOf(session), text.trim(), now()));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
    }

    /** 文章创建事件 → 广播系统消息(演示 WS 与业务通过 Spring 事件联动)。 */
    @EventListener
    public void onArticleCreated(ArticleCreatedEvent event) {
        broadcast(new ChatMessage("system", "📝 新文章:《" + event.title() + "》 by " + event.author(), now()));
    }

    /** 同步广播,避免多线程并发写同一 WS 会话。 */
    private synchronized void broadcast(ChatMessage message) {
        String json;
        try {
            json = objectMapper.writeValueAsString(message);
        } catch (IOException e) {
            return;
        }
        TextMessage payload = new TextMessage(json);
        for (WebSocketSession session : sessions) {
            try {
                if (session.isOpen()) {
                    session.sendMessage(payload);
                }
            } catch (IOException e) {
                log.warn("WS 发送失败,移除会话", e);
                sessions.remove(session);
            }
        }
    }

    private String usernameOf(WebSocketSession session) {
        Object username = session.getAttributes().get("username");
        return username == null ? "匿名" : username.toString();
    }

    private String now() {
        return LocalTime.now().withNano(0).toString();
    }
}
