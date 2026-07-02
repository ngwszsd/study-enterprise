package com.study.exception;

/** 未认证/凭据错误,映射为 HTTP 401。 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
