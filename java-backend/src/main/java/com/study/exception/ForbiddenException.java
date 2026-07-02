package com.study.exception;

/** 无权限操作(如非作者改删文章),映射为 HTTP 403。 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
