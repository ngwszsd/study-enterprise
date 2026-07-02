package com.study.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

/** 统一错误响应:机读 code + 可读 message,校验错误附字段级 errors。 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(String code, String message, Map<String, String> errors) {

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, null);
    }
}
