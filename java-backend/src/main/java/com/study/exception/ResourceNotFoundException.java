package com.study.exception;

/** 资源不存在,映射为 HTTP 404。 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
