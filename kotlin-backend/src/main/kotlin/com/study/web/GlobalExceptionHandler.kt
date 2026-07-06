package com.study.web

import com.study.exception.ConflictException
import com.study.exception.ForbiddenException
import com.study.exception.InvalidFileException
import com.study.exception.ResourceNotFoundException
import com.study.exception.UnauthorizedException
import com.study.web.dto.ErrorResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.multipart.MaxUploadSizeExceededException

/**
 * 全局异常处理:领域异常翻译为统一错误响应,不吞异常。
 *
 * @RestControllerAdvice 会拦截所有 @RestController 抛出的异常,并把返回值写成 JSON。
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(javaClass)

    // @ExceptionHandler: 指定这个函数处理哪类异常;@ResponseStatus 指定 HTTP 状态码。
    @ExceptionHandler(MethodArgumentNotValidException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleValidation(ex: MethodArgumentNotValidException): ErrorResponse {
        val fields = LinkedHashMap<String, String>()
        ex.bindingResult.fieldErrors.forEach { fields.putIfAbsent(it.field, it.defaultMessage ?: "无效") }
        return ErrorResponse("VALIDATION_ERROR", "参数校验失败", fields)
    }

    @ExceptionHandler(UnauthorizedException::class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    fun handleUnauthorized(ex: UnauthorizedException) = ErrorResponse("UNAUTHORIZED", ex.message)

    @ExceptionHandler(ForbiddenException::class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    fun handleForbidden(ex: ForbiddenException) = ErrorResponse("FORBIDDEN", ex.message)

    @ExceptionHandler(ResourceNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleNotFound(ex: ResourceNotFoundException) = ErrorResponse("NOT_FOUND", ex.message)

    @ExceptionHandler(ConflictException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handleConflict(ex: ConflictException) = ErrorResponse("CONFLICT", ex.message)

    @ExceptionHandler(InvalidFileException::class, MaxUploadSizeExceededException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleInvalidFile(ex: Exception): ErrorResponse {
        val message = if (ex is MaxUploadSizeExceededException) "文件过大" else ex.message
        return ErrorResponse("INVALID_FILE", message)
    }

    @ExceptionHandler(Exception::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleUnexpected(ex: Exception): ErrorResponse {
        log.error("未处理的服务器异常", ex)
        return ErrorResponse("INTERNAL_ERROR", "服务器内部错误")
    }
}
