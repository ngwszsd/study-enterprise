package com.study.event;

/** 文章创建领域事件;WS 与 SSE 都监听它做实时推送(用 Spring 事件解耦)。 */
public record ArticleCreatedEvent(Long id, String title, String author) {
}
