package com.study.ws

/** WebSocket 聊天消息。from 为 "system" 表示系统消息。 */
data class ChatMessage(val from: String, val text: String, val time: String)
