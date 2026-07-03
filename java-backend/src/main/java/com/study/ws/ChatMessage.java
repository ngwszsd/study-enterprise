package com.study.ws;

/** WebSocket 聊天消息。from 为 "system" 表示系统消息。 */
public record ChatMessage(String from, String text, String time) {
}
