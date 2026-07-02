package com.study.exception;

/** 非法文件(过大/类型不符),映射为 HTTP 400。 */
public class InvalidFileException extends RuntimeException {

    public InvalidFileException(String message) {
        super(message);
    }
}
